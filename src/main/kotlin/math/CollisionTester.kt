package math

import models.Model
import java.awt.Color
import java.awt.Graphics
import java.awt.Polygon
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.JPanel

class CollidableObject(val points : List<Vector2>, var position: Vector2, var rotation: Double, var speed: Vector2 = Vector2(0.0, 0.0), var rotSpeed: Double = 0.0)

fun main() {
    val frame = JFrame("Collision Tester")

    val objects = ArrayList<CollidableObject>()

    val player = CollidableObject(Model.SQUARE.asVectors(), Vector2(0.0, 0.0), 0.0)

    objects.add(CollidableObject(Model.SQUARE.asVectors(), Vector2(1.0, 0.0), 0.0))
    objects.add(player)

    val panel = object : JPanel(){
        override fun paint(g: Graphics) {
            super.paint(g)
            g.color = Color.BLACK
            g.clearRect(0, 0, width, height)

            for (collidableObject in objects) {

                val toUI: (Vector2) -> Vector2 = {it -> (it * 50.0).floor() + Vector2(width.toDouble()/2.0, height.toDouble()/2.0)}
                val toPolygon: (List<Vector2>) -> Polygon = {points ->
                    val xPoints = points.map {it.getX().toInt()}
                    val yPoints = points.map {it.getY().toInt()}
                    val nPoints = points.size
                    Polygon(xPoints.toIntArray(), yPoints.toIntArray(), nPoints)
                }

                val transformedPoints = collidableObject.points.map { it.rotate(collidableObject.rotation) + collidableObject.position }
                val uiPoints = transformedPoints.map {toUI(it)}

                if(collidableObject != player){
                    g.color = Color.RED
                    g.drawPolygon(toPolygon(uiPoints))

                    val playerPoints = player.points.map { it.rotate(player.rotation) + player.position }
                    val playerUiPoints = playerPoints.map {toUI(it)}

                    val mtv = getCollisionMTV(playerUiPoints, uiPoints)

                    if(mtv != null){
                        //Flip y axis to make more readable
                        println("mtv : ${Vector2(mtv.getX(), -mtv.getY())}")
                        val playerPos = toUI(player.position)
                        val vecPos = playerPos + mtv
                        g.drawLine(playerPos.getX().toInt(), playerPos.getY().toInt(), vecPos.getX().toInt(), vecPos.getY().toInt())

                        val newPlayerUIPoints = playerUiPoints.map {it + mtv*1.1}
                        g.color = Color.CYAN
                        g.drawPolygon(toPolygon(newPlayerUIPoints))
                    }

                }else{
                    g.color = Color.BLUE
                    g.drawPolygon(toPolygon(uiPoints))
                }
            }
        }
    }

    frame.add(panel)

    Thread {
        while(true) {
            Thread.sleep(16)
            panel.repaint()
            frame.repaint()
        }
    }.start()

    frame.addKeyListener(object: KeyListener{
        override fun keyTyped(e: KeyEvent?) {
        }

        override fun keyPressed(e: KeyEvent) {
            if(e.keyCode == KeyEvent.VK_Q){
                player.rotation += 0.03;
            }
            if(e.keyCode == KeyEvent.VK_E){
                player.rotation -= 0.03;
            }
            if(e.keyCode == KeyEvent.VK_W){
                player.position += Vector2(0.0, -0.02);
            }
            if(e.keyCode == KeyEvent.VK_A){
                player.position += Vector2(-0.02, 0.0);
            }
            if(e.keyCode == KeyEvent.VK_S){
                player.position += Vector2(0.0, 0.02);
            }
            if(e.keyCode == KeyEvent.VK_D){
                player.position += Vector2(0.02, 0.0);
            }
        }

        override fun keyReleased(e: KeyEvent?) {
        }

    })

    frame.setSize(600, 600)
    frame.isVisible = true

}