package logic.harvest

import screeps.api.*

class LMHarvestGetWayFromPosToPos {
    fun gets(fPos1: RoomPosition, fPos2: RoomPosition, inSwampCost: Int = 10, inPlainCost: Int = 2, safeMove: Boolean = false): PathFinder.Path {
        fun roomCallback(roomName: String): CostMatrix {
            val room: Room = Game.rooms[roomName] ?: return PathFinder.CostMatrix()
            val costs = PathFinder.CostMatrix()
            room.find(FIND_MY_STRUCTURES).forEach { struct ->
                if (struct.structureType === STRUCTURE_ROAD) {
                    costs.set(struct.pos.x, struct.pos.y, 1)
                } else if (struct.structureType !== STRUCTURE_CONTAINER &&
                        (struct.structureType !== STRUCTURE_RAMPART)) {
                    costs.set(struct.pos.x, struct.pos.y, 0xff)
                }
            }

            room.find(FIND_STRUCTURES).forEach { struct ->
                if (struct.structureType == STRUCTURE_ROAD)
                    costs.set(struct.pos.x, struct.pos.y, 1)
            }

            room.find(FIND_CONSTRUCTION_SITES).forEach { cs ->
                if (cs.structureType == STRUCTURE_ROAD)
                    costs.set(cs.pos.x, cs.pos.y, 1)
            }

            room.find(FIND_HOSTILE_STRUCTURES).forEach { struct ->
                costs.set(struct.pos.x, struct.pos.y, 0xff)
            }

            if (safeMove) {
                room.find(FIND_HOSTILE_STRUCTURES).filter { it.structureType == STRUCTURE_KEEPER_LAIR }.forEach { keeper ->
                    for (dx in (keeper.pos.x - 5)..(keeper.pos.x + 5))
                        for (dy in (keeper.pos.y - 5)..(keeper.pos.y + 5)) {
                            if (dx !in 0..49 || dy !in 0..49) continue
                            costs.set(dx, dy, 0xff)
                        }
                }
            }
            return costs
        }

        val goals = GoalWithRange (
            pos = fPos2,
            range  = 1
        )

        return PathFinder.search(fPos1, goals, options {
            maxOps = 5000
            maxRooms = 6
            plainCost = inPlainCost
            swampCost = inSwampCost
            roomCallback = ::roomCallback
        })
    }
}