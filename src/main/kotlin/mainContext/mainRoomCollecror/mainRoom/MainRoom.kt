package mainContext.mainRoomCollecror.mainRoom

import battleGroup.BattleGroup
import mainContext.MainContext
import mainContext.constants.MainRoomConstant
import mainContext.constants.SlaveRoomConstant
import mainContext.mainRoomCollecror.mainRoom.slaveRoom.SlaveRoom
import mainContext.constants.path.CacheCarrier
import mainContext.dataclass.BgSpawnResult
import mainContext.dataclass.QueueSpawnRecord
import mainContext.mainRoomCollecror.MainRoomCollector
import screeps.api.*
import screeps.api.structures.*
import screeps.utils.toMap
import kotlin.math.min
import kotlin.random.Random

class MainRoom(val mc: MainContext, val mrCol: MainRoomCollector, val name: String, val describe: String, val constant: MainRoomConstant) {
    val room: Room = Game.rooms[this.name] ?: throw AssertionError("Not room $this.name")
    val slaveRooms: MutableMap<String, SlaveRoom> = mutableMapOf()

    val need = Array(3) { Array(100) { 0 } }
    val have = Array(100) { 0 }
    private val haveForQueue = Array(100) { 0 }
    val queue = mutableListOf<QueueSpawnRecord>()

    //Cash data
    val resStorage: MutableMap<ResourceConstant, Int> = mutableMapOf()  //Stored in Store + Logist
    val resTerminal: MutableMap<ResourceConstant, Int> = mutableMapOf()

    var creepNeedUpgradeID: String = ""
    var creepNeedUpgradeResource: ResourceConstant? = null
    var creepNeedUpgradeResourceQuantity: Int = 0
    var needMineral: MutableMap<ResourceConstant, Int> = mutableMapOf()

    var spawnForBattleGroup: BattleGroup? = null

    //StructureSpawn
    private var _structureSpawn: Map<String, StructureSpawn>? = null
    val structureSpawn: Map<String, StructureSpawn>
        get() {
            if (this._structureSpawn == null)
                _structureSpawn = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_SPAWN && it.isActive() }.associate { it.id to it as StructureSpawn }
            return _structureSpawn ?: throw AssertionError("Error get StructureSpawn")
        }

    //StructureExtension
    private var _structureExtension: Map<String, StructureExtension>? = null
    val structureExtension: Map<String, StructureExtension>
        get() {
            if (this._structureExtension == null) {
                _structureExtension = mc.lm.cash.mr.getFromCashExt(this)
                if (_structureExtension == null) {
                    mc.lm.cash.mr.needSave()
                    _structureExtension = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_EXTENSION && it.isActive() }.associate { it.id to it as StructureExtension }
                }
            }
            return _structureExtension ?: throw AssertionError("Error get StructureExtension")
        }

    //StructureController
    private var _structureController: Map<Int, StructureController>? = null
    val structureController: Map<Int, StructureController>
        get() {
            if (this._structureController == null)
                _structureController = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_CONTROLLER && it.isActive() }.withIndex().associate { it.index to it.value as StructureController }
            return _structureController ?: throw AssertionError("Error get StructureController")
        }

    //Source
    private var _source: Map<Int, Source>? = null
    val source: Map<Int, Source>
        get() {
            if (this._source == null)
                _source = this.room.find(FIND_SOURCES).sortedWith(Comparator<Source> { a, b ->
                    when {
                        a.id > b.id -> 1
                        a.id < b.id -> -1
                        else -> 0
                    }
                }).withIndex().associate { it.index to it.value }
            return _source ?: throw AssertionError("Error get Source")
        }

    //ConstructionSite
    private var _constructionSite: Map<String, ConstructionSite>? = null
    val constructionSite: Map<String, ConstructionSite>
        get() {
            if (this._constructionSite == null)
                _constructionSite = this.room.find(FIND_CONSTRUCTION_SITES).associateBy { it.id }
            return _constructionSite ?: throw AssertionError("Error get ConstructionSite")
        }

    //StructureTower
    private var _structureTower: Map<String, StructureTower>? = null
    val structureTower: Map<String, StructureTower>
        get() {
            if (this._structureTower == null)
                _structureTower = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_TOWER && it.isActive() }.associate { it.id to it as StructureTower }
            return _structureTower ?: throw AssertionError("Error get StructureTower")
        }

    //StructureContainer
    private var _structureContainer: Map<String, StructureContainer>? = null
    val structureContainer: Map<String, StructureContainer>
        get() {
            if (_structureContainer == null) {
                _structureContainer = mc.lm.cash.mr.getFromCashCnt(this)
                if (_structureContainer == null) {
                    mc.lm.cash.mr.needSave()
                    _structureContainer = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_CONTAINER && it.isActive() }.associate { it.id to it as StructureContainer }
                }
            }

            return _structureContainer ?: throw AssertionError("Error get StructureContainer")
        }

    //StructureContainerNearSource
    private var _structureContainerNearSource: Map<Int, StructureContainer>? = null //id source
    val structureContainerNearSource: Map<Int, StructureContainer>
        get() {
            if (_structureContainerNearSource == null) {
                _structureContainerNearSource = mc.lm.cash.mr.getFromCashCntS(this)
                if (_structureContainerNearSource == null) {
                    mc.lm.cash.mr.needSave()
                    val resultContainer = mutableMapOf<Int, StructureContainer>()
                    for (sourceRec in this.source)
                        for (container in this.structureContainer.values)
                            if (!resultContainer.containsValue(container) && sourceRec.value.pos.inRangeTo(container.pos, 1))
                                resultContainer[sourceRec.key] = container
                    _structureContainerNearSource = resultContainer
                }
            }
            return _structureContainerNearSource
                    ?: throw AssertionError("Error get StructureContainerNearSource")
        }

    //StructureContainerNearController
    private var _structureContainerNearController: Map<Int, StructureContainer>? = null //id source
    val structureContainerNearController: Map<Int, StructureContainer>
        get() {
            if (_structureContainerNearController == null) {
                _structureContainerNearController = mc.lm.cash.mr.getFromCashCntC(this)
                if (_structureContainerNearController == null) {
                    mc.lm.cash.mr.needSave()
                    val resultContainer = mutableMapOf<Int, StructureContainer>()
                    for (container in this.structureContainer.values) {
                        val protectStructureController: StructureController? = this.structureController[0]
                        if (protectStructureController != null
                                && !this.structureContainerNearSource.containsValue(container)
                                && !this.structureContainerNearMineral.containsValue(container)
                                && protectStructureController.pos.inRangeTo(container.pos, 3))
                            resultContainer[0] = container
                    }
                    _structureContainerNearController = resultContainer
                }
            }
            return _structureContainerNearController
                    ?: throw AssertionError("Error get StructureContainerNearController")
        }

    //StructureStorage
    private var _structureStorage: Map<Int, StructureStorage>? = null
    val structureStorage: Map<Int, StructureStorage>
        get() {
            if (this._structureStorage == null)
                _structureStorage = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_STORAGE && it.isActive() }.withIndex().associate { it.index to it.value as StructureStorage }
            return _structureStorage ?: throw AssertionError("Error get StructureStorage")
        }

    //StructureLink
    private var _structureLink: Map<String, StructureLink>? = null
    private val structureLink: Map<String, StructureLink>
        get() {
            if (this._structureLink == null)
                _structureLink = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_LINK && it.isActive() }.associate { it.id to it as StructureLink }
            return _structureLink ?: throw AssertionError("Error get StructureLink")
        }

    //StructureLinkNearSource
    private var _structureLinkNearSource: Map<Int, StructureLink>? = null
    val structureLinkNearSource: Map<Int, StructureLink>
        get() {
            if (this._structureLinkNearSource == null) {
                val resultLink = mutableMapOf<Int, StructureLink>()
                for (sourceRec in this.source)
                    for (link in this.structureLink.values)
                        if (!resultLink.containsValue(link) && sourceRec.value.pos.inRangeTo(link.pos, 2))
                            resultLink[sourceRec.key] = link
                _structureLinkNearSource = resultLink
            }
            return _structureLinkNearSource
                    ?: throw AssertionError("Error get StructureLinkNearSource")
        }

    //StructureLinkNearController
    private var _structureLinkNearController: Map<Int, StructureLink>? = null
    val structureLinkNearController: Map<Int, StructureLink>
        get() {
            if (this._structureLinkNearController == null) {
                val resultLink = mutableMapOf<Int, StructureLink>()
                for (link in this.structureLink.values) {
                    val protectStructureController: StructureController? = this.structureController[0]
                    if (protectStructureController != null && !this.structureLinkNearSource.containsValue(link) && protectStructureController.pos.inRangeTo(link.pos, 3))
                        resultLink[0] = link
                }
                _structureLinkNearController = resultLink
            }
            return _structureLinkNearController
                    ?: throw AssertionError("Error get StructureLinkNearController")
        }

    //StructureLinkNearStorage
    private var _structureLinkNearStorage: Map<Int, StructureLink>? = null
    val structureLinkNearStorage: Map<Int, StructureLink>
        get() {
            if (this._structureLinkNearStorage == null) {
                val resultLink = mutableMapOf<Int, StructureLink>()
                for (link in this.structureLink.values) {
                    val protectStructureStorage: StructureStorage? = this.structureStorage[0]
                    if (protectStructureStorage != null
                            && !this.structureLinkNearSource.containsValue(link)
                            && !this.structureLinkNearController.containsValue(link)
                            && protectStructureStorage.pos.inRangeTo(link.pos, 3))
                        resultLink[0] = link
                }
                _structureLinkNearStorage = resultLink
            }
            return _structureLinkNearStorage
                    ?: throw AssertionError("Error get StructureLinkNearStorage")
        }


    //StructureTerminal
    private var _structureTerminal: Map<Int, StructureTerminal>? = null
    val structureTerminal: Map<Int, StructureTerminal>
        get() {
            if (this._structureTerminal == null)
                _structureTerminal = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_TERMINAL && it.isActive() }.withIndex().associate { it.index to it.value as StructureTerminal }
            return _structureTerminal ?: throw AssertionError("Error get StructureTerminal")
        }

    //StructureNuker
    private var _structureNuker: Map<Int, StructureNuker>? = null
    val structureNuker: Map<Int, StructureNuker>
        get() {
            if (this._structureNuker == null)
                _structureNuker = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_NUKER && it.isActive() }.withIndex().associate { it.index to it.value as StructureNuker }
            return _structureNuker ?: throw AssertionError("Error get StructureNuker")
        }

    //StructureFactory
    private var _structureFactory: Map<Int, StructureFactory>? = null
    val structureFactory: Map<Int, StructureFactory>
        get() {
            if (this._structureFactory == null)
                _structureFactory = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_FACTORY && it.isActive() }.withIndex().associate { it.index to it.value as StructureFactory }
            return _structureFactory ?: throw AssertionError("Error get StructureFactory")
        }

    //StructurePowerSpawn
    private var _structurePowerSpawn: Map<Int, StructurePowerSpawn>? = null
    val structurePowerSpawn: Map<Int, StructurePowerSpawn>
        get() {
            if (this._structurePowerSpawn == null)
                _structurePowerSpawn = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_POWER_SPAWN && it.isActive() }.withIndex().associate { it.index to it.value as StructurePowerSpawn }
            return _structurePowerSpawn ?: throw AssertionError("Error get StructureFactory")
        }

    //StructureObserver
    private var _structureObserver: Map<Int, StructureObserver>? = null
    val structureObserver: Map<Int, StructureObserver>
        get() {
            if (this._structureObserver == null)
                _structureObserver = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_OBSERVER && it.isActive() }.withIndex().associate { it.index to it.value as StructureObserver }
            return _structureObserver ?: throw AssertionError("Error get StructureObserver")
        }

    //Mineral
    private var _mineral: Mineral? = null
    val mineral: Mineral
        get() {
            if (this._mineral == null)
                _mineral = this.room.find(FIND_MINERALS).first()
            return _mineral ?: throw AssertionError("Error get Mineral")
        }

    //StructureExtractor
    private var _structureExtractor: Map<Int, StructureExtractor>? = null
    val structureExtractor: Map<Int, StructureExtractor>
        get() {
            if (this._structureExtractor == null)
                _structureExtractor = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_EXTRACTOR && it.isActive() }.withIndex().associate { it.index to it.value as StructureExtractor }
            return _structureExtractor ?: throw AssertionError("Error get StructureExtractor")
        }

    //StructureContainerNearMineral
    private var _structureContainerNearMineral: Map<Int, StructureContainer>? = null
    val structureContainerNearMineral: Map<Int, StructureContainer>
        get() {
            if (_structureContainerNearMineral == null) {
                _structureContainerNearMineral = mc.lm.cash.mr.getFromCashCntM(this)
                if (_structureContainerNearMineral==null) {
                    mc.lm.cash.mr.needSave()
                    val resultContainer = mutableMapOf<Int, StructureContainer>()
                    for (container in this.structureContainer.values) {
                        if (!this.structureContainerNearSource.containsValue(container) && this.mineral.pos.inRangeTo(container.pos, 1))
                            resultContainer[0] = container
                    }
                    _structureContainerNearMineral = resultContainer
                }
            }
            return _structureContainerNearMineral
                    ?: throw AssertionError("Error get StructureContainerNearMineral")
        }

    //StructureLabs
    private var _structureLab: Map<String, StructureLab>? = null
    val structureLab: Map<String, StructureLab>
        get() {
            if (this._structureLab == null) {
                _structureLab = mc.lm.cash.mr.getFromCashLab(this)
                if (_structureLab==null) {
                    mc.lm.cash.mr.needSave()
                    _structureLab = this.room.find(FIND_STRUCTURES).filter { it.structureType == STRUCTURE_LAB && it.isActive() }.associate { it.id to it as StructureLab }
                }
            }
            return _structureLab ?: throw AssertionError("Error get StructureLab")
        }

    //StructureLabsSort
    private var _structureLabSort: Map<Int, StructureLab>? = null
    val structureLabSort: Map<Int, StructureLab>
        get() {
            if (this._structureLabSort == null) {
                this._structureLabSort = mc.lm.production.lmLabMainRoomGetLabSorted.getLabSort(structureLab)
            }
            return _structureLabSort ?: throw AssertionError("Error get StructureLabSort")
        }

    //upgradeLab
    private var _upgradeLab: StructureLab? = null
    val upgradeLab: StructureLab?
        get() {
            if (this._upgradeLab == null) {
                if (this.structureLabSort.size >= 3) {
                    _upgradeLab = this.structureLabSort[2]
                    _upgradeLabIndexSorted = 2
                } else {
                    _upgradeLab = structureLab.values.firstOrNull {
                        (it.store[RESOURCE_ENERGY] ?: 0) > 0
                    } ?: this.structureLab.values.firstOrNull()
                }
            }
            return _upgradeLab
        }

    //upgradeLabIndexSorted
    private var _upgradeLabIndexSorted: Int? = null
    val upgradeLabIndexSorted: Int
        get() {
            if (this._upgradeLabIndexSorted == null) {
                val locUpgradeLab = upgradeLab
                if (locUpgradeLab != null) {
                    _upgradeLabIndexSorted = structureLabSort.filter { it.value.pos.isEqualTo(locUpgradeLab.pos) }.keys.firstOrNull()
                            ?: -1
                }
            }
            return _upgradeLabIndexSorted ?: -1
        }

    private fun buildCreeps() {
        this.needCorrection()
        for (slaveRoomRecord in this.slaveRooms) slaveRoomRecord.value.needCorrection()
        this.buildQueue()

        this.spawnCreep()
    }

    private fun needCorrection() {
        val nowLevelOfRoom: Int = this.getLevelOfRoom()

        if (nowLevelOfRoom < this.constant.levelOfRoom) {
            if (this.getResourceInStorage() > 40000) {
                if (this.need[0][5] == 0) this.need[0][5] = 1 //filler
                if (this.need[1][5] == 0) this.need[1][5] = 1 //filler
                this.need[0][8] = 2
                mc.lm.building.lmBuildingSnapShot.restoreSnapShot(this)
                return
            } else {
                mc.lm.building.lmBuildingSnapShot.restoreSnapShot(this)
                this.constant.levelOfRoom = nowLevelOfRoom
            }
        } else this.constant.levelOfRoom = nowLevelOfRoom

        when (this.constant.levelOfRoom) {
            0 -> this.needCorrection0()
            1 -> this.needCorrection1()
            2 -> this.needCorrection2()
            3 -> this.needCorrection3()
        }
    }

    fun getResourceInStorage(resource: ResourceConstant = RESOURCE_ENERGY): Int {
        return this.resStorage[resource] ?: 0
    }

    fun getResourceInTerminal(resource: ResourceConstant = RESOURCE_ENERGY): Int {
        return this.resTerminal[resource] ?: 0
    }

    fun getResource(resource: ResourceConstant = RESOURCE_ENERGY): Int {
        return (this.resTerminal[resource] ?: 0) + (this.resStorage[resource] ?: 0)
    }

    private fun getTimeDeath(fRole: Int): Int {
        return when (fRole) {
            2 -> mc.lm.lmHarvestCacheRecordRoom.gets("mainContainer0", this)?.timeForDeath
                    ?: 0
            4 -> mc.lm.lmHarvestCacheRecordRoom.gets("mainContainer1", this)?.timeForDeath
                    ?: 0
            1, 3 -> 10
            else -> 0
        }
    }


    private fun buildQueue() {
        for (i in this.have.indices) this.haveForQueue[i] = this.have[i]
        val fPriorityOfRole = if (this.getResourceInStorage() < 2000) arrayOf(0, 9, 1, 3, 2, 4, 14, 5, 6, 30, 31, 20, 21, 22, 7, 19, 10, 8, 11, 12, 13, 15, 16, 17, 18)
        else arrayOf(0, 9, 5, 14, 30, 31, 1, 3, 2, 4, 20, 21, 22, 6, 7, 19, 10, 8, 11, 12, 13, 15, 16, 17, 18)

        //Main 0..1
        for (priority in 0..1) {
            for (fRole in fPriorityOfRole) {
                var fNeed = this.need[0][fRole]
                if (priority >= 1) fNeed += this.need[1][fRole]
                if (priority >= 2) fNeed += this.need[2][fRole]
                while (this.haveForQueue[fRole] < fNeed) {
                    this.haveForQueue[fRole]++
                    this.queue.add(QueueSpawnRecord(fRole, this.name, this.name, this.getTimeDeath(fRole)))
                }
            }
        }

        //BattleGroup
        this.queue.add(QueueSpawnRecord(99, this.name, this.name, 0))


        //Slave 0
        for (slaveRoom in this.slaveRooms.values) {
            for (i in slaveRoom.have.indices) slaveRoom.haveForQueue[i] = slaveRoom.have[i]
            slaveRoom.buildQueue(this.queue, 0)
        }

        //Slave 1
        for (slaveRoom in this.slaveRooms.values) {
            slaveRoom.buildQueue(this.queue, 1)
        }

        //Main 2
        for (priority in 2..2) {
            for (fRole in fPriorityOfRole) {
                var fNeed = this.need[0][fRole]
                if (priority >= 1) fNeed += this.need[1][fRole]
                if (priority >= 2) fNeed += this.need[2][fRole]
                while (this.haveForQueue[fRole] < fNeed) {
                    this.haveForQueue[fRole]++
                    this.queue.add(QueueSpawnRecord(fRole, this.name, this.name, this.getTimeDeath(fRole)))
                }
            }
        }

        //Slave 2
        for (slaveRoom in slaveRooms.values)
            slaveRoom.buildQueue(this.queue, 2)
    }

    private fun getBodyRole(role: Int): Array<BodyPartConstant> {
        var result: Array<BodyPartConstant> = arrayOf()

        when (role) {
            0 -> {
                if (this.have[0] == 0 && this.room.energyAvailable < 800) result = arrayOf(MOVE, MOVE, WORK, CARRY)
                else if (this.room.energyCapacityAvailable < 400) result = arrayOf(MOVE, MOVE, WORK, CARRY)
                else if (this.room.energyCapacityAvailable < 800) result = arrayOf(MOVE, MOVE, WORK, WORK, CARRY, CARRY)
                else result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, CARRY, CARRY, CARRY, CARRY)
            }

            1, 3 -> {
                result = arrayOf(MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY)
            }

            2 -> {
                val carrierAuto: CacheCarrier? = mc.lm.lmHarvestCacheRecordRoom.gets("mainContainer0", this)
                if (carrierAuto == null) {
                    mc.lm.messenger.log("ERROR", this.name, "Auto not exists mainContainer0", COLOR_RED)
                    result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                } else {
                    result = carrierAuto.needBody
                }
            }

            4 -> {
                val carrierAuto: CacheCarrier? = mc.lm.lmHarvestCacheRecordRoom.gets("mainContainer1", this)
                if (carrierAuto == null) {
                    mc.lm.messenger.log("ERROR", this.name, "Auto not exists mainContainer1", COLOR_RED)
                    result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                } else {
                    result = carrierAuto.needBody
                }
            }

            5 -> {
                if (this.room.energyCapacityAvailable >= 5000) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else if (this.room.energyCapacityAvailable >= 1300) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else result = arrayOf(MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            6 -> {
                if (this.room.energyCapacityAvailable < 1800) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else if (this.room.energyCapacityAvailable <= 5600) result = arrayOf(CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE)
                else result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            7 -> {
                //ToDO remove after 3 level room
                val controller = this.structureController[0]
                if (controller != null && controller.level == 8) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else if (this.room.energyCapacityAvailable < 1800) result = arrayOf(MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY)
                else if (this.room.energyCapacityAvailable < 2300) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY)
                else if (this.room.energyCapacityAvailable < 3000) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY)
                else if (this.room.energyCapacityAvailable <= 5600) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            8 -> {
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            9 -> {
                result = arrayOf(CARRY, CARRY, MOVE)
            }

            10 -> {
                if (this.room.energyCapacityAvailable >= 3500) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            11 -> {
                if (this.room.energyCapacityAvailable >= 3500) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
                else result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            13 -> {
                result = arrayOf(WORK, CARRY, MOVE)
            }

            14 -> {
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            15 -> {
                if (this.room.energyCapacityAvailable in 2300..6399) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK)
                if (this.room.energyCapacityAvailable >= 6400) result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK)
            }

            16 -> {
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            17 -> {
                result = arrayOf(MOVE, CARRY)
            }

            18 -> {
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            19 -> {
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, WORK, CARRY, CARRY, CARRY, CARRY, CARRY, CARRY)
            }

            20 -> {
                //Min
                //result = arrayOf(MOVE)
                //Middle
                //result = arrayOf(MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK,ATTACK)
                //Max
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK, ATTACK)
            }

            21 -> { //RA
                //Min
                //result = arrayOf(MOVE, MOVE)
                //Middle
                //result = arrayOf(MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK,RANGED_ATTACK)
                //Max
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK)
            }

            22 -> {
                //Min
                //result = arrayOf(MOVE, MOVE, MOVE)
                //Middle
                //result = arrayOf(TOUGH,TOUGH,TOUGH,TOUGH,TOUGH,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,MOVE,HEAL,HEAL,HEAL,HEAL,HEAL,HEAL,HEAL,HEAL,HEAL,HEAL)
                //Max
                result = arrayOf(TOUGH, TOUGH, TOUGH, TOUGH, TOUGH, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL, HEAL)
            }

            30 -> {
                result = arrayOf(MOVE, ATTACK)
            }

            31 -> {
                result = arrayOf(MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, MOVE, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK, RANGED_ATTACK)
            }
        }
        return result
    }

    private fun spawnCreep() {
        if (!this.constant.creepSpawn) return
        for (spawn in this.structureSpawn.values) {
            if (this.queue.size == 0) return
            if (!spawn.isActive() || spawn.spawning != null) continue
            var result: ScreepsReturnCode = OK

            //Battle group
            if (this.queue[0].role == 99) {
                val resultBgSpawn = mc.battleGroupContainer.spawnCreep(this, spawn)
                if (resultBgSpawn == BgSpawnResult.StartSpawn) continue
                if (resultBgSpawn == BgSpawnResult.CantSpawn) return
                if (resultBgSpawn == BgSpawnResult.QueueEmpty) {
                    this.queue.removeAt(0)
                    if (this.queue.size == 0) return
                }
            }

            val d: dynamic = object {}
            d["role"] = this.queue[0].role
            d["slaveRoom"] = this.queue[0].slaveRoom
            d["mainRoom"] = this.queue[0].mainRoom
            d["tickDeath"] = this.queue[0].timeDeath
            val spawnOptions: dynamic = object {}
            spawnOptions["memory"] = d

            if (this.queue[0].slaveRoom == this.queue[0].mainRoom)
                result = spawn.spawnCreep(getBodyRole(this.queue[0].role), "mst_${this.queue[0].mainRoom}_${this.queue[0].slaveRoom}_${Game.time} ", spawnOptions.unsafeCast<SpawnOptions>())
            else {
                val slaveRoom = this.slaveRooms[this.queue[0].slaveRoom]
                if (slaveRoom != null)
                    result = spawn.spawnCreep(slaveRoom.getBodyRole(this.queue[0].role), "mst_${this.queue[0].mainRoom}_${this.queue[0].slaveRoom}_${Game.time} ", spawnOptions.unsafeCast<SpawnOptions>())
            }
            if (result == OK) {
                if (this.queue[0].slaveRoom != this.queue[0].mainRoom) {
                    val slaveRoom: SlaveRoom? = this.slaveRooms[this.queue[0].slaveRoom]
                    slaveRoom?.profitMinus(this.queue[0].role)
                }

                this.queue.removeAt(0)
            }
        }
    }

    fun getSpawnOrExtensionForFiling(pos: RoomPosition, mainContext: MainContext): Structure? {
        data class StructureData(val need: Int, val priority: Int)

        val needs: MutableMap<Structure, StructureData> = mutableMapOf()

        // Загружаем все extension
        for (extension in this.structureExtension.values)
            if (extension.store.getCapacity(RESOURCE_ENERGY) ?: 0 > extension.store[RESOURCE_ENERGY] ?: 0)
                needs[extension] = StructureData(extension.store.getCapacity(RESOURCE_ENERGY)
                        ?: 0 - (extension.store[RESOURCE_ENERGY] ?: 0), 1)

        // Загружаем все спавны
        for (spawn in this.structureSpawn.values)
            if (spawn.store.getCapacity(RESOURCE_ENERGY) ?: 0 > spawn.store[RESOURCE_ENERGY] ?: 0)
                needs[spawn] = StructureData(spawn.store.getCapacity(RESOURCE_ENERGY)
                        ?: 0 - (spawn.store[RESOURCE_ENERGY] ?: 0), 1)

        // Загружаем Tower если енергия меньше 1000


        if (this.constant.roomHostile) {
            if (this.queue.size < 2) {
                for (tower in this.structureTower.values)
                    if (tower.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 >= 300) needs[tower] = StructureData(tower.store.getCapacity(RESOURCE_ENERGY)
                            ?: 0 - (tower.store[RESOURCE_ENERGY] ?: 0), 0)
            } else {
                for (tower in this.structureTower.values)
                    if (tower.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 >= 300) needs[tower] = StructureData(tower.store.getCapacity(RESOURCE_ENERGY)
                            ?: 0 - (tower.store[RESOURCE_ENERGY] ?: 0), 3)
            }
        } else {
            for (tower in this.structureTower.values)
                if (tower.store.getFreeCapacity(RESOURCE_ENERGY) ?: 0 >= 300) needs[tower] = StructureData(tower.store.getCapacity(RESOURCE_ENERGY)
                        ?: 0 - (tower.store[RESOURCE_ENERGY] ?: 0), 3)
        }


        if (needs.isEmpty()) return null
        // Производим коррекцию с учетем заданий которые делаются и ищем ближайший
        var resultObject: Structure? = null
        var resultRange = 1000
        var resultPriority = 1000

        for (need in needs) {
            if (need.value.need > mainContext.tasks.getEnergyCaringTo(need.key.id)) {
                val tTmpRange = pos.getRangeTo(need.key.pos)
                val tTmpPriority = need.value.priority
                if ((tTmpPriority < resultPriority) || (tTmpRange < resultRange && tTmpPriority == resultPriority)) {
                    resultRange = tTmpRange
                    resultPriority = tTmpPriority
                    resultObject = need.key
                }
            }
        }
        return resultObject
    }

    fun getConstructionSite(pos: RoomPosition): ConstructionSite? {
        var tObject: ConstructionSite? = null
        var tMinRange = 1000
        for (construct in this.constructionSite.values) {
            val tTmpRange = pos.getRangeTo(construct.pos)
            if (tTmpRange < tMinRange) {
                tMinRange = tTmpRange
                tObject = construct
            }
        }
        return tObject
    }

    fun getSourceForHarvest(pos: RoomPosition, mainContext: MainContext): Source {
        var tSource: Source = this.source.values.first()
        var tDistance = 1000
        for (source in this.source.values) {
            val tRangeTmp = pos.getRangeTo(source.pos)
            if (tRangeTmp < tDistance && mainContext.tasks.getSourceHarvestNum(source.id) < 5 && source.energy > 100) {
                tDistance = tRangeTmp
                tSource = source
            }
        }
        return tSource
    }

    //0 - only Role 0 creep
    //1 - Storage, 3 container, energy >300+20*50 1300
    fun getLevelOfRoom(): Int {
        if (this.room.energyCapacityAvailable >= 5600
                && this.structureLinkNearSource.size == this.source.size
                && this.structureLinkNearController.size == 1
                && this.structureStorage.size == 1
                && this.structureTerminal.size == 1
                && this.structureLinkNearStorage.size == 1
                && (this.structureController[0]?.level ?: 0) == 8
        ) return 3

        if (this.room.energyCapacityAvailable >= 1800
                && this.structureLinkNearSource.size == this.source.size
                && this.structureContainerNearController.size == 1
                && this.structureStorage.size == 1
                && this.structureTerminal.size == 1
                && this.structureLinkNearStorage.size == 1
        ) return 2

        if (this.room.energyCapacityAvailable >= 1300 &&
                this.structureContainerNearSource.size == this.source.size &&
                this.structureContainerNearController.size == 1 &&
                this.structureStorage.size == 1)
            return 1

        return 0
    }

    fun getTicksToDowngrade(): Int {
        var result = 0
        val protectedStructureController = this.structureController[0]
        if (protectedStructureController != null) result = protectedStructureController.ticksToDowngrade
        return result
    }

    init {
        mc.constants.accountInit.initMainRoom(this)
        this.constant.slaveRooms.forEachIndexed { index, slaveName ->
            val slaveRoomConstant: SlaveRoomConstant? = this.constant.slaveRoomConstantContainer[slaveName]
            if (slaveRoomConstant != null)
                slaveRooms[slaveName] = SlaveRoom(mc, this, slaveName, "${this.describe}S$index", slaveRoomConstant)
            else mc.lm.messenger.log("ERROR", "${this.name} $slaveName", "initialization don't see constant", COLOR_RED)
        }
    }

    private fun setNextTickRun(): Boolean {
        if (this.constant.roomRunNotEveryTickNextTickRun > Game.time) return false
        this.constant.roomRunNotEveryTickNextTickRun = Game.time + Random.nextInt(mc.constants.globalConstant.roomRunNotEveryTickTicksPauseMin,
                mc.constants.globalConstant.roomRunNotEveryTickTicksPauseMax)
        mc.lm.messenger.log("TEST", this.name, "Main room not every tick run. Next tick: ${this.constant.roomRunNotEveryTickNextTickRun}", COLOR_GREEN)
        return true
    }

    private fun setHostile() {
        val towerInvader = this.room.find(FIND_HOSTILE_STRUCTURES).firstOrNull { it.structureType == STRUCTURE_TOWER }

        if (towerInvader != null) {
            this.constant.roomHostileType = 4
            this.constant.roomHostileNum = 5
            this.constant.roomHostile = true
        } else {
            val invader = this.room.find(FIND_HOSTILE_STRUCTURES).firstOrNull { it.structureType == STRUCTURE_INVADER_CORE }
            if (invader != null) {
                this.constant.roomHostileType = 3
                this.constant.roomHostileNum = 1
                this.constant.roomHostile = true
            } else {
                val hostileCreeps = this.room.find(FIND_HOSTILE_CREEPS).filter { it.name.startsWith("invader") }
                this.constant.roomHostile = hostileCreeps.isNotEmpty()
                var typeAttack = 2 //ranged
                for (hostileCreep in hostileCreeps)
                    if (hostileCreep.body.firstOrNull { it.type == ATTACK } != null) {
                        typeAttack = 1
                        break
                    }
                this.constant.roomHostileType = typeAttack
                this.constant.roomHostileNum = hostileCreeps.size
            }
        }
    }


    fun runInStartOfTick() {
        this.setHostile()
        this.manualDefenceInStartOfTick()
        this.setMineralNeed()
        this.alarmStorage()
    }

    fun runInEndOfTick() {
        if (this.constant.levelOfRoom > 1) this.runLinkTransfers()
        this.runTower()
        this.buildCreeps()

        for (room in this.slaveRooms.values) {
            try {
                room.runInEndOfTick()
            } catch (e: Exception) {
                mc.lm.messenger.log("ERROR", "Slave room in end", room.name, COLOR_RED)
            }
        }

        val cpuStartLab = Game.cpu.getUsed()
        Memory["CPULab"] = Memory["CPULab"] + Game.cpu.getUsed() - cpuStartLab
    }

    private fun alarmStorage() {
        val storage: StructureStorage = this.structureStorage[0] ?: return
        if (storage.store.values.sum() > 800000) {
            mc.lm.messenger.log("INFO", this.name, "Storage full", COLOR_RED)
        }
    }

    fun runNotEveryTick() {
        for (record in this.slaveRooms) {
            try {
                record.value.runNotEveryTick()
            } catch (e: Exception) {
                mc.lm.messenger.log("ERROR", "Slave not every tick", record.value.name, COLOR_RED)
            }
        }


        if (!this.setNextTickRun()) return
        mc.lm.building.lmBuildingSnapShot.restoreSnapShot(this)
        this.building()
        this.needCleanerCalculate()
    }

    fun missingStructures(): String {
        if (this.constructionSite.isNotEmpty()) return ""
        val controller: StructureController = this.structureController[0] ?: return ""


        if (controller.level == 2) {
            if (this.room.energyCapacityAvailable != 550) return "Extension"
        }

        if (controller.level == 3) {
            if (this.room.energyCapacityAvailable != 800) return "Extension"
            if (this.structureTower.size != 1) return "Tower"
            if (this.source.isNotEmpty() && !this.structureContainerNearSource.containsKey(0)) return "Cont. source 0"
            if (this.source.size > 1 && !this.structureContainerNearSource.containsKey(1)) return "Cont. source 1"
        }

        if (controller.level == 4) {
            if (this.room.energyCapacityAvailable != 1300) return "Extension"
            if (this.structureTower.size != 1) return "Tower"
            if (this.source.isNotEmpty() && !this.structureContainerNearSource.containsKey(0)) return "Cont. source 0"
            if (this.source.size > 1 && !this.structureContainerNearSource.containsKey(1)) return "Cont. source 1"
            if (!this.structureContainerNearController.containsKey(0)) return "Cont. controller"
            if (!this.structureStorage.containsKey(0)) return "Storage"
        }

        if (controller.level == 5) {
            if (this.room.energyCapacityAvailable != 1800) return "Extension"
            if (this.structureTower.size != 2) return "Tower"
            if (this.source.isNotEmpty() && !this.structureContainerNearSource.containsKey(0)) return "Cont. source 0"
            if (this.source.size > 1 && !this.structureContainerNearSource.containsKey(1)) return "Cont. source 1"
            if (!this.structureContainerNearController.containsKey(0)) return "Cont. controller"
            if (!this.structureStorage.containsKey(0)) return "Storage"
        }

        if (controller.level == 6) {
            if (this.room.energyCapacityAvailable != 2300) return "Extension"
            if (this.structureTower.size != 2) return "Tower"
            if (this.source.isNotEmpty() && !this.structureLinkNearSource.containsKey(0)) return "Link source 0"
            if (this.source.size > 1 && !this.structureLinkNearSource.containsKey(1)) return "Link source 1"
            if (!this.structureContainerNearController.containsKey(0)) return "Cont. controller"
            if (!this.structureStorage.containsKey(0)) return "Storage"
            if (this.structureTerminal.size != 1) return "Terminal"
            if (this.structureLinkNearStorage.size != 1) return "Link storage"
            if (this.structureContainerNearSource.containsKey(0)) return "-Cont. source 0"
            if (this.structureContainerNearSource.containsKey(1)) return "-Cont. source 1"
            if (this.structureExtractor.size != 1) return "Extractor"
            if (this.structureContainerNearMineral.size != 1) return "Cont. mineral"
            if (this.structureLab.size < 3) return "Lab"
        }

        if (controller.level == 7) {
            if (this.structureSpawn.size != 2) return "Spawn"
            if (this.room.energyCapacityAvailable != 5600) return "Extension"
            if (this.structureTower.size != 3) return "Tower"
            if (this.source.isNotEmpty() && !this.structureLinkNearSource.containsKey(0)) return "Link source 0"
            if (this.source.size > 1 && !this.structureLinkNearSource.containsKey(1)) return "Link source 1"
            if (!this.structureContainerNearController.containsKey(0)) return "Cont. controller"
            if (!this.structureStorage.containsKey(0)) return "Storage"
            if (this.structureTerminal.size != 1) return "Terminal"
            if (this.structureLinkNearStorage.size != 1) return "Link storage"
            if (this.structureContainerNearSource.containsKey(0)) return "-Cont. source 0"
            if (this.structureContainerNearSource.containsKey(1)) return "-Cont. source 1"
            if (this.structureExtractor.size != 1) return "Extractor"
            if (this.structureContainerNearMineral.size != 1) return "Cont. mineral"
            if (this.structureLab.size < 6) return "Lab"
        }

        if (controller.level == 8) {
            if (this.structureLinkNearController.size != 1) return "Link controller"
            if (this.structureContainerNearController.containsKey(0)) return "-Cont. controller"
            if (this.structureSpawn.size != 3) return "Spawn"
            if (this.room.energyCapacityAvailable != 12900) return "Extension"
            if (this.structureTower.size != 6) return "Tower"
            if (this.source.isNotEmpty() && !this.structureLinkNearSource.containsKey(0)) return "Link source 0"
            if (this.source.size > 1 && !this.structureLinkNearSource.containsKey(1)) return "Link source 1"
            if (!this.structureStorage.containsKey(0)) return "Storage"
            if (this.structureTerminal.size != 1) return "Terminal"
            if (this.structureLinkNearStorage.size != 1) return "Link storage"
            if (this.structureContainerNearSource.containsKey(0)) return "-Cont. source 0"
            if (this.structureContainerNearSource.containsKey(1)) return "-Cont. source 1"
            if (this.structureExtractor.size != 1) return "Extractor"
            if (this.structureContainerNearMineral.size != 1) return "Cont. mineral"
            if (this.structureLab.size < 10) return "Lab"
            if (this.structureObserver.size != 1) return "Observer"
            if (this.structureFactory.size != 1) return "Factory"
            if (this.structurePowerSpawn.size != 1) return "Power spawn"
            if (this.structureNuker.size != 1) return "Nuker"
        }
        return ""
    }

    private fun runLinkTransfers() {
        val levelOfRoom = this.getLevelOfRoom()
        if (levelOfRoom < 3 || this.have[19] == 0) {
            val fLinkTo: StructureLink = this.structureLinkNearStorage[0] ?: return
            if (fLinkTo.store[RESOURCE_ENERGY] ?: 0 != 0) return

            for (link in this.structureLinkNearSource.values)
                if (link.store[RESOURCE_ENERGY] ?: 0 >= 700 && link.cooldown == 0) {
                    link.transferEnergy(fLinkTo, link.store[RESOURCE_ENERGY] ?: 0)
                    break
                }
            return
        }

        if (levelOfRoom == 3) {
            if (this.source.size == 2) {
                val fLinkStorage: StructureLink = this.structureLinkNearStorage[0] ?: return
                val fLinkController: StructureLink = this.structureLinkNearController[0] ?: return

                for (link in this.structureLinkNearSource.values)
                    if (link.store[RESOURCE_ENERGY] ?: 0 >= 500 && link.cooldown == 0 && fLinkController.cooldown == 0 && fLinkController.store[RESOURCE_ENERGY] ?: 0 < 300) {
                        link.transferEnergy(fLinkController, min(link.store[RESOURCE_ENERGY]
                                ?: 0, fLinkController.store.getCapacity(RESOURCE_ENERGY)
                                ?: 0 - (fLinkController.store[RESOURCE_ENERGY] ?: 0)))

                        return
                    }

                for (link in this.structureLinkNearSource.values)
                    if (link.store[RESOURCE_ENERGY] ?: 0 >= 700 && link.cooldown == 0 && fLinkStorage.cooldown == 0 && fLinkStorage.store[RESOURCE_ENERGY] ?: 0 == 0) {
                        link.transferEnergy(fLinkStorage, link.store[RESOURCE_ENERGY] ?: 0)
                        break
                    }
            }

            if (this.source.size == 1) {

                val fLinkStorage: StructureLink = this.structureLinkNearStorage[0] ?: return
                val fLinkController: StructureLink = this.structureLinkNearController[0] ?: return
                val fLinkSource: StructureLink = this.structureLinkNearSource[0] ?: return
                if (fLinkSource.store[RESOURCE_ENERGY] ?: 0 >= 500 && fLinkSource.cooldown == 0 && fLinkController.cooldown == 0 && fLinkController.store[RESOURCE_ENERGY] ?: 0 < 300) {
                    fLinkSource.transferEnergy(fLinkController, min(fLinkSource.store[RESOURCE_ENERGY]
                            ?: 0, fLinkController.store.getCapacity(RESOURCE_ENERGY)
                            ?: 0 - (fLinkController.store[RESOURCE_ENERGY] ?: 0)))
                    return
                }
                if (fLinkStorage.store[RESOURCE_ENERGY] ?: 0 > 0 && fLinkStorage.cooldown == 0 && fLinkController.cooldown == 0 && fLinkController.store[RESOURCE_ENERGY] ?: 0 == 0) {
                    fLinkStorage.transferEnergy(fLinkController, fLinkStorage.store[RESOURCE_ENERGY]
                            ?: 0)
                    return
                }
            }
        }
    }

    fun needCleanWhat(store: Store?, resource: ResourceConstant): ResourceConstant? {
        if (store == null) return null
        for (record in store.keys)
            if (store[record] != null && store[record] != 0 && record != resource) return record
        return null
    }

    private fun needClean(store: Store?, resource: ResourceConstant): Boolean {
        if (store == null) return false
        return (store[resource] ?: 0) != (store.toMap().map { it.value }.sum())
    }

    fun needCleanerCalculate() {
        var result = false
        if (!result) result = needClean(this.structureContainerNearSource[0]?.store, RESOURCE_ENERGY)
        if (!result) result = needClean(this.structureContainerNearSource[1]?.store, RESOURCE_ENERGY)
        if (!result) result = needClean(this.structureContainerNearSource[2]?.store, RESOURCE_ENERGY)
        if (!result) result = needClean(this.structureContainerNearController[0]?.store, RESOURCE_ENERGY)
        if (!result) result = needClean(this.structureContainerNearMineral[0]?.store, this.mineral.mineralType)
        this.constant.needCleaner = result
    }

    private fun setMineralNeed() {
        if (this.constant.reactionActive != "") {
            val reaction = this.constant.reactionActive.unsafeCast<ResourceConstant>()
            if (this.structureLabSort.size !in arrayOf(3, 6, 10)) return
            val reactionComponent = mc.constants.globalConstant.labReactionComponent[reaction]
                    ?: return
            this.needMineral[reactionComponent[0]] = (this.needMineral[reactionComponent[0]]
                    ?: 0) + 2000
            this.needMineral[reactionComponent[1]] = (this.needMineral[reactionComponent[1]]
                    ?: 0) + 2000
        }

        if (this.creepNeedUpgradeID != "") {
            val resource: ResourceConstant = this.creepNeedUpgradeResource.unsafeCast<ResourceConstant>()
            this.needMineral[resource] = (this.needMineral[resource]
                    ?: 0) + this.creepNeedUpgradeResourceQuantity
        }
    }
}


