package com.ftorterolo.config

import java.util.concurrent._

import com.ftorterolo.util.JsonUtil
import org.slf4j.LoggerFactory

class Detector_003  extends DetectorQueues {

  val logger = LoggerFactory.getLogger(this.getClass)
  val sendExecutor = Executors.newSingleThreadScheduledExecutor()
  val receiveExecutor = Executors.newSingleThreadScheduledExecutor()
  val vector_D003:Array[Int] = Array(0,0,0,0)

  def start(initialDelay: Long, period: Long, unit: TimeUnit) = {
    sendExecutor.scheduleAtFixedRate(sendRunner, initialDelay, period, TimeUnit.SECONDS)
    receiveExecutor.scheduleAtFixedRate(receiveRunner, initialDelay + 10, period/3, TimeUnit.SECONDS)
  }

  def shutdown() = {
    sendExecutor.shutdown()
    receiveExecutor.shutdown()
  }

  def sendRunner= new Runnable() {
    override def run(): Unit = {

      //vector are 0-based
      vector_D003.update(2,vector_D003(2)+1)
      val lamport = JsonUtil.toJson(vector_D003)

      val r = scala.util.Random
      val (autosR, motosR, escuelaR, omnibusR)= (r.nextInt(10), r.nextInt(10), r.nextInt(5),r.nextInt(10))
      val trafico = Map(Transportes.Auto.nombre -> autosR, Transportes.Moto.nombre -> motosR, Transportes.Escuela.nombre -> escuelaR, Transportes.Omnibus.nombre -> omnibusR )

      val d3_to_d1 = Mensaje(emisor=Detectores.D003.id, receptor=Detectores.D001.id,trafico)
      val d3_to_d2 = Mensaje(emisor=Detectores.D003.id, receptor=Detectores.D002.id,trafico)
      val d3_to_d4 = Mensaje(emisor=Detectores.D003.id, receptor=Detectores.D004.id,trafico)

      MessageHandler.sendMessage(queueUrlD001,JsonUtil.toJson(d3_to_d1), lamport)
      MessageHandler.sendMessage(queueUrlD002,JsonUtil.toJson(d3_to_d2), lamport)
      MessageHandler.sendMessage(queueUrlD004,JsonUtil.toJson(d3_to_d4), lamport)
//      println(s"$lamport")
    }
  }

  def receiveRunner = new Runnable() {
    override def run(): Unit = {
      val messages = MessageHandler.receiveMessage(queueUrlD003)

      if (messages.nonEmpty) {
        val mensaje = JsonUtil.fromJson[Mensaje](messages.head.getBody)
        val lamport = JsonUtil.fromJson[(Int, Int, Int, Int)](messages.head.getMessageAttributes.get(MessageHandler.Lamport).getStringValue)

        if (mensaje.receptor.equals(Detectores.D003.id)) {
          vector_D003.update(0, Math.max(vector_D003(0), lamport._1))
          vector_D003.update(1, Math.max(vector_D003(1), lamport._2))
          vector_D003.update(2, vector_D003(2) + 1)
          vector_D003.update(3, Math.max(vector_D003(3), lamport._4))
          // save message and delete from the quede
          MessageHandler.deleteMessage(queueUrlD003,messages.head.getReceiptHandle)
        }
        else {
          logger.error("This message is in wrong place")
        }
      }
    }
  }

}