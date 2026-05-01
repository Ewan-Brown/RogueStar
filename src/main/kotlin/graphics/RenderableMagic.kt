package graphics

import graphics.Graphics.IntermediaryRenderable
import graphics.Graphics.Renderable
import math.HasReferenceFrame
import math.InReferenceFrame
import math.Pose
import math.ReferenceFrame
import math.Transform
import math.combineTransforms
import math.getTransformLocalToParentFrame
import kotlin.collections.forEach

interface HasNestedRenderables<R1 : ReferenceFrame, R2 : ReferenceFrame> : InReferenceFrame<R1>, HasReferenceFrame<R2>{
    fun getImmediateRenderables() : List<IntermediaryRenderable<R2>>
    fun getChildren() : List<HasNestedRenderables<R2, *>> = emptyList()
}

fun <R0: ReferenceFrame, R1: ReferenceFrame> processRenderables(node: HasNestedRenderables<R0, R1>, consumer: (Renderable) -> Unit) {
    val localToParent = getTransformLocalToParentFrame(node)
    node.getImmediateRenderables().forEach { it ->
        val newPose: Pose<R0> = it.pose.applyTransform(localToParent)
        val renderable = Renderable(it.model, newPose.coordinate.getVector(), newPose.orientation.getAngle(), newPose.zHeight.getZ(), it.scale, it.colorData, it.metaData)
        consumer(renderable)
    }
    node.getChildren().forEach { node ->
        cascadingProcess(node, localToParent, consumer)
    }
}

private fun <R0 : ReferenceFrame, R1: ReferenceFrame, R2: ReferenceFrame> cascadingProcess(node : HasNestedRenderables<R1, R2>, parentToRoot: Transform<R1, R0>, consumer: (Renderable) -> Unit){
    val localToParent = getTransformLocalToParentFrame(node)
    val localToRoot: Transform<R2, R0> = combineTransforms(localToParent, parentToRoot)
    node.getImmediateRenderables().forEach { it ->
        val newPose: Pose<R0> = it.pose.applyTransform(localToRoot)
        val renderable = Renderable(it.model, newPose.coordinate.getVector(), newPose.orientation.getAngle(), newPose.zHeight.getZ(), it.scale, it.colorData, it.metaData)
        consumer(renderable)
    }
    node.getChildren().forEach { node ->
        cascadingProcess(node, localToRoot, consumer)
    }
}