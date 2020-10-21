package accounts.srvMain2

import mainContext.constants.Constants
import mainContext.dataclass.SlaveRoomType
import screeps.api.BodyPartConstant
import screeps.api.ResourceConstant
import screeps.api.WORK

fun AccountInitMain2.initHeadOut(const: Constants) {
                                               //M0     M1     M2     M3     M4     M5     M6
    const.initMainRoomConstantContainer(arrayOf("W5N3","W4N3","W6N3","W7N3","W7N4","W7N5","W3N5"))

    const.getMainRoomConstant("W5N3").initSlaveRoomConstantContainer(arrayOf("W5N2","W6N3"))          //M0
    const.getMainRoomConstant("W4N3").initSlaveRoomConstantContainer(arrayOf("W4N2","W3N5"))          //M1
    const.getMainRoomConstant("W6N3").initSlaveRoomConstantContainer(arrayOf())                       //M2
    const.getMainRoomConstant("W7N3").initSlaveRoomConstantContainer(arrayOf("W8N3"))                 //M3
    const.getMainRoomConstant("W7N4").initSlaveRoomConstantContainer(arrayOf("W8N4"))                 //M4
    const.getMainRoomConstant("W7N5").initSlaveRoomConstantContainer(arrayOf("W8N5","W7N6"))          //M5
    const.getMainRoomConstant("W3N5").initSlaveRoomConstantContainer(arrayOf())                       //M6



}

fun AccountInitMain2.initBodyOut(const: Constants) {
    const.m(0).useCash = false
    const.m(1).useCash = false
    const.m(2).useCash = false
    const.m(3).useCash = false
    const.m(4).useCash = false
    const.m(5).useCash = false
    const.m(6).useCash = false
    const.globalConstant.terminalPriorityRoom = "W5N3"

    const.m(5).defenceMinHits = 100_000
    const.s(5,1).autoBuildRoad = true

    const.m(2).creepUseUpgraderAndBuilderFromMainRoom = true
    const.s(0,1).model = SlaveRoomType.Colonize

    const.m(6).creepUseUpgraderAndBuilderFromMainRoom = true
    const.s(1,1).model = SlaveRoomType.Colonize
    const.s(1,1).pathToRoom = arrayOf("W4N3","W4N2","W5N2","W5N1","W4N1","W3N1","W3N2","W3N3","W2N3","W2N4","W2N5","W3N5")


    const.globalConstant.defenceLimitUpgrade = 10000000
    const.globalConstant.balanceQtyUpgraderDefault = 12
    const.globalConstant.balanceQtyBuilderDefault = 0
    const.globalConstant.nukerFill = true

    const.globalConstant.username = "vsolo0"

    const.globalConstant.creepUpgradablePartsRange[19] = mapOf<BodyPartConstant, List<Pair<Int, ResourceConstant>>>(
            WORK to
                    listOf(
                            Pair(100000, "XGH2O".unsafeCast<ResourceConstant>()),
                            Pair(300000, "GH2O".unsafeCast<ResourceConstant>()),
                            Pair(300000, "GH".unsafeCast<ResourceConstant>())
                    )
    )

    const.globalConstant.creepUpgradablePartsRange[7] = mapOf<BodyPartConstant, List<Pair<Int, ResourceConstant>>>(
            WORK to
                    listOf(
                            Pair(2000, "XGH2O".unsafeCast<ResourceConstant>()),
                            Pair(300000, "GH2O".unsafeCast<ResourceConstant>()),
                            Pair(300000, "GH".unsafeCast<ResourceConstant>())
                    )
    )

    const.globalConstant.creepUpgradablePartsRange[101] = mapOf<BodyPartConstant, List<Pair<Int, ResourceConstant>>>(
            WORK to
                    listOf(
                            Pair(10000, "XLH2O".unsafeCast<ResourceConstant>()),
                            Pair(10000, "LH2O".unsafeCast<ResourceConstant>()),
                            Pair(1000, "LH".unsafeCast<ResourceConstant>())
                    )
    )

    const.globalConstant.creepUpgradablePartsRange[10] = mutableMapOf<BodyPartConstant, List<Pair<Int, ResourceConstant>>>(
            WORK to
                    listOf(
                            Pair(2000, "XLH2O".unsafeCast<ResourceConstant>()),
                            Pair(2000, "LH2O".unsafeCast<ResourceConstant>()),
                            Pair(1000, "LH".unsafeCast<ResourceConstant>())
                    )
    )
}