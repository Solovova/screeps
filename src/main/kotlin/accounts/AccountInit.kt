package accounts

import mainContext.MainContext
import mainContext.constants.Constants
import mainContext.mainRoomCollecror.mainRoom.MainRoom
import mainContext.mainRoomCollecror.mainRoom.slaveRoom.SlaveRoom

open class AccountInit {
    open fun initHead(constants: Constants) {

    }

    open fun initBody(constants: Constants) {

    }

    open fun initMainRoom(mainRoom: MainRoom) {

    }

    open fun initSlaveRoom(slaveRoom: SlaveRoom) {

    }

    open fun initMineral(mainContext: MainContext) {

    }

    //call after fill mineral and production
    open fun initTuning(mainContext: MainContext) {

    }

}