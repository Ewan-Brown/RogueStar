package math

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

//https://dyn4j.org/2010/01/sat/

//TODO make sure to check for containment!
fun getCollisionMTV(p1 : List<Vector2>, p2: List<Vector2>) : Vector2? {
    val projTest1 = projectionTest(p1, p2) ?: return null
    val projTest2 = projectionTest(p2, p1) ?: return null

    //Important to flip one of these, as they are coming from opposing perspectives :)
    val mtv = if (projTest1.getMagnitude() < projTest2.getMagnitude() ) projTest1 * -1.0 else projTest2

    //Attempt to find the point of contact, if it exists

    return mtv
}

private fun projectionTest(p1 : List<Vector2>, p2 : List<Vector2>) : Vector2? {
    val normals = getSideNormals(p1)
    var minimumOverlap: Vector2? = null;
    for (normal in normals) {
        val projection1 = getProjection(p1, normal)
        val projection2 = getProjection(p2, normal)
        val overlap = getOverlap(projection1, projection2)
        if(overlap == null) {
            return null;
        }else if(overlap > 0 && (minimumOverlap == null || overlap < minimumOverlap.getMagnitude())) {
            minimumOverlap = normal * overlap;
        }

    }
    return minimumOverlap
}

fun getSideNormals(points: List<Vector2>) : List<Vector2> {
    val normals = mutableListOf<Vector2>()
    for (i in points.indices) {
        val v1 = points[i]
        val v2 = if(i == points.lastIndex) points[0] else points[i + 1]
        val edge = v2 - v1
        val normal = edge.leftHandNormal()
        normals.add(normal.normalize())
    }
    return normals
}

data class Projection(val min: Double, val max: Double){
    override fun toString(): String {
        return "{$min - $max}"
    }
}

fun getOverlap(projection: Projection, otherProjection: Projection) : Double? {
    if(projection.max <= otherProjection.min){
        return null
    }else if(projection.min >= otherProjection.max){
        return null
    }else{
        return projection.max - otherProjection.min
    }
}

fun getProjection(p : List<Vector2>, normal: Vector2) : Projection {
    var min = normal.dot(p[0])
    var max = min

    for(i in 1..<p.size) {
        val v = p[i]
        val projVal = normal.dot(v)
        if(projVal < min){
            min = projVal
        }else if(projVal > max){
            max = projVal
        }
    }

    return Projection(min, max)
}