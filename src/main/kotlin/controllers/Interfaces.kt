package controllers

import math.Coordinates
import math.EntityReferenceFrame
import math.Vector2
import physics.ControllableEntity
import physics.SimpleShip
import physics.Station
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


interface PlantInterface{
    fun update()
}

sealed interface SimpleInterface : PlantInterface{
    abstract fun setDesiredThrust(thrust: Vector2)
    abstract fun setDesiredTorque(t: Double)
    abstract fun setFiring(f: Boolean)
}

sealed interface ExampleAlternativeInterface : PlantInterface{
    abstract fun setSomeVariable(thrust: Vector2)
    abstract fun setAnotherThing(t: Double)
    abstract fun setYetAnotherThing(f: Boolean)
}

@OptIn(ExperimentalUuidApi::class)
abstract class Job(){
    var assignedPawnUUID : Uuid? = null

//    abstract fun canPerform(p : AbstractPawn) : Boolean
    abstract fun isComplete() : Boolean
}

class MoveJob(val destination: Coordinates<EntityReferenceFrame>) : Job() {
    override fun isComplete(): Boolean {
        TODO()
    }
}

class ManStationJob(val station: Station) : Job() {
    override fun isComplete(): Boolean {
        return false
    }

    var isFulfilled = false

}

abstract class DirectControllerInterface<S: ControllableEntity>(protected val target: S) : PlantInterface

@OptIn(ExperimentalUuidApi::class)
abstract class PawnedControllerInterface<S: ControllableEntity>(protected val target: S) : PlantInterface{
    private val jobs = mutableListOf<Job>()
    private val idlePawnList: MutableList<Uuid> = target.getPawnsInside().map{it.UUID}.toMutableList()

    override fun update() {
        jobs.removeIf(Job::isComplete)
        for(job in jobs){
            if(job.assignedPawnUUID != null){
                val pawn = target.getPawnsInside().find { it.UUID == job.assignedPawnUUID }
                if(pawn == null){
                    throw IllegalStateException("A pawn with ID ${job.assignedPawnUUID} was found assigned to a job $job but the pawn is not present in the entity $target")
                }
                when (job){
                    is MoveJob -> {
                        TODO()
                    }
                    is ManStationJob -> {
                        val pawnLoc = pawn.getCoordinates()
                        val stationLoc = job.station.getCoordinates()
                        var pawnToStation = stationLoc - pawnLoc
                        if(pawnToStation.getMagnitude() < 0.01){
                            job.isFulfilled = true
                            pawnToStation = Vector2()
                        }else{
                            job.isFulfilled = false
                            var magnitude = pawnToStation.getMagnitude()
                            if(magnitude > pawn.getMaxMovementSpeed()) pawnToStation = pawnToStation.normalize();
                        }
                        pawn.setVelocity(pawnToStation * pawn.getMaxMovementSpeed())
                    }
                }

            }
        }
    }

    fun attemptToAssign(j: Job){
        if(j.assignedPawnUUID != null){
            throw Exception("Attempted to assign a pawn to job $j, but pawn ${j.assignedPawnUUID} is already assigned to it!")
        }
        else if(idlePawnList.isEmpty()){
            println("Tried to assign to job: $j, but no idle pawns available")
        }else{
            val p = idlePawnList.first()
            print("assigning pawn $p to job $j")
            j.assignedPawnUUID = p
            idlePawnList.remove(p)
        }
    }

    fun addJob(j: Job){
        jobs.add(j)
    }

    fun removeJob(j: Job){
        jobs.remove(j)
    }
}

class SimpleDirectInterface(target: SimpleShip) : DirectControllerInterface<SimpleShip>(target), SimpleInterface{
    override fun setDesiredThrust(thrust: Vector2) {
        target.getThrusters().forEach {
            it.setOrientation(thrust)
            it.setThrottle(thrust.getMagnitude())
        }
    }

    override fun setDesiredTorque(t: Double) {
        target.getTorquers().forEach {
            it.setTorque(t)
        }
    }

    override fun setFiring(f: Boolean) {
        target.getGuns().forEach {
            it.toggleFiring(f)
        }
    }

    override fun update() {
        //Nothing to do!
    }

}

@OptIn(ExperimentalUuidApi::class)
class SimplePawnedInterface(target: SimpleShip) : PawnedControllerInterface<SimpleShip>(target), SimpleInterface{

    val pilotJob = ManStationJob(target.navStation)
    val gunnerJob = ManStationJob(target.weaponStation)

    init{
        addJob(pilotJob)
        addJob(gunnerJob)
    }

    override fun setDesiredThrust(thrust: Vector2) {
        if(pilotJob.assignedPawnUUID != null){
            if(pilotJob.isFulfilled) {
                target.getThrusters().forEach {
                    it.setOrientation(thrust)
                    it.setThrottle(thrust.getMagnitude())
                }
            }
        }else{
            attemptToAssign(pilotJob)
        }
    }

    override fun setDesiredTorque(t: Double) {
        if(pilotJob.assignedPawnUUID != null){
            if(pilotJob.isFulfilled) {
                target.getTorquers().forEach {
                    it.setTorque(t)
                }
            }
        }else{
            attemptToAssign(pilotJob)
        }
    }

    override fun setFiring(f: Boolean) {
        if(gunnerJob.assignedPawnUUID != null){
            if(gunnerJob.isFulfilled) {
                target.getGuns().forEach {
                    it.toggleFiring(f)
                }
            }
        }else{
            attemptToAssign(gunnerJob)
        }
    }
}