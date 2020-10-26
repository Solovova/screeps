package accounts.srvMain2

import mainContext.constants.Constants
import mainContext.dataclass.SlaveRoomType
import screeps.api.BodyPartConstant
import screeps.api.ResourceConstant
import screeps.api.WORK

fun AccountInitMain2.initHeadOut(const: Constants) {
                                               //M0     M1     M2     M3     M4     M5     M6
    const.initMainRoomConstantContainer(arrayOf("W5N3","W4N3","W6N3","W7N3","W7N4","W7N5","W3N5"))

    const.getMainRoomConstant("W5N3").initSlaveRoomConstantContainer(arrayOf("W5N2","W5N4"))          //M0
    const.getMainRoomConstant("W4N3").initSlaveRoomConstantContainer(arrayOf("W4N2","W4N4"))          //M1
    const.getMainRoomConstant("W6N3").initSlaveRoomConstantContainer(arrayOf("W6N4"))                 //M2
    const.getMainRoomConstant("W7N3").initSlaveRoomConstantContainer(arrayOf("W8N3"))                 //M3
    const.getMainRoomConstant("W7N4").initSlaveRoomConstantContainer(arrayOf("W8N4"))                 //M4
    const.getMainRoomConstant("W7N5").initSlaveRoomConstantContainer(arrayOf("W8N5","W7N6","W6N5"))   //M5
    const.getMainRoomConstant("W3N5").initSlaveRoomConstantContainer(arrayOf("W2N5","W3N6","W4N5"))   //M6



}

fun AccountInitMain2.initBodyOut(const: Constants) {
//    const.m(0).useCash = false
//    const.m(1).useCash = false
//    const.m(2).useCash = false
//    const.m(3).useCash = false
//    const.m(4).useCash = false
//    const.m(5).useCash = false
//    const.m(6).useCash = false
    const.globalConstant.terminalPriorityRoom = ""

    //const.s(0,2).model = SlaveRoomType.Colonize

    const.globalConstant.defenceLimitUpgrade = 17000000
    const.globalConstant.balanceQtyUpgraderDefault = 7 //lvl3Room + lvl2Room*5 - max
    const.globalConstant.balanceQtyBuilderDefault = 2   //(lvl3Room + lvl2Room) / 3 - min
    const.globalConstant.nukerFill = false

    const.globalConstant.username = "vsolo0"

    const.m(0).reactionActiveArr = arrayOf("ZK", "UL", "G", "GH", "OH", "GH2O", "XGH2O")
    const.m(1).reactionActiveArr = arrayOf("ZK", "UL", "G", "GH", "OH", "GH2O", "XGH2O")
    const.m(2).reactionActiveArr = arrayOf("ZK", "UL", "G", "GH", "OH", "GH2O", "XGH2O")
    const.m(3).reactionActiveArr = arrayOf("ZK", "UL", "G", "GH", "OH", "GH2O", "XGH2O")
    const.m(4).reactionActiveArr = arrayOf("ZK", "UL", "G", "GH", "OH", "GH2O", "XGH2O")
    const.m(5).reactionActiveArr = arrayOf("LH", "LH2O", "XLH2O", "GH2O", "XGH2O")
    const.m(6).reactionActiveArr = arrayOf("LH", "LH2O", "XLH2O", "GH2O", "XGH2O")

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
                            Pair(100000, "XGH2O".unsafeCast<ResourceConstant>()),
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