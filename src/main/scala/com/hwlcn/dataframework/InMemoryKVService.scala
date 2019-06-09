package com.hwlcn.dataframework

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorRef, Stash}
import akka.cluster.Cluster
import akka.cluster.ddata.Replicator._
import akka.cluster.ddata.{DistributedData, LWWMap, LWWMapKey}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.TimeoutException
import scala.concurrent.duration.Duration

/**
  * 通过akka的数据分发机制实现kv的分布式缓存
  *
  * @author huangweili
  */
class InMemoryKVService extends Actor with Stash {

  import InMemoryKVService._

  private val KV_SERVICE = "kvservice"

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val replicator = DistributedData(context.system).replicator
  private implicit val node = DistributedData(context.system).selfUniqueAddress
  private implicit val cluster: Cluster = Cluster(context.system)

  private val timeout = Duration(15, TimeUnit.SECONDS)
  private val readMajority = ReadMajority(timeout)
  private val writeMajority = WriteMajority(timeout)


  /**
    * 定义key的 group 信息
    *
    * @param group
    * @return
    */
  private def groupKey(group: String): LWWMapKey[Any, Any] = {
    LWWMapKey[Any, Any](KV_SERVICE + "_" + group)
  }

  def receive: Receive = kvService

  def kvService: Receive = {

    case GetKV(group: String, key: String) => {
      val request = Request(sender(), key)
      replicator ! Get(groupKey(group), readMajority, Some(request))
    }
    case success@GetSuccess(group: LWWMapKey[Any@unchecked, Any@unchecked],
    Some(request: Request)) => {
      val appData = success.get(group)
      request.client ! GetKVSuccess(request.key, appData.get(request.key).orNull)
    }

    case NotFound(group: LWWMapKey[Any@unchecked, Any@unchecked], Some(request: Request)) =>
      request.client ! GetKVSuccess(request.key, null)

    case GetFailure(_, Some(request: Request)) =>
      val error = s"获取key:${request.key}的数据异常。"
      logger.error(error)
      request.client ! GetKVFailed(new Exception(error))

    case PutKV(group: String, key: String, value: Any) =>
      val request = Request(sender(), key)
      val update = Update(groupKey(group), LWWMap(), writeMajority, Some(request)) { map =>
        map :+ (key -> value)
      }
      replicator ! update
    case UpdateSuccess(_, Some(request: Request)) =>
      request.client ! PutKVSuccess
    case ModifyFailure(_, error, cause,
    Some(request: Request)) =>
      request.client ! PutKVFailed(request.key, new Exception(error, cause))
    case UpdateTimeout(_, Some(request: Request)) =>
      request.client ! PutKVFailed(request.key, new TimeoutException())

    case DeleteKVGroup(group: String) =>
      replicator ! Delete(groupKey(group), writeMajority)
    case DeleteSuccess(group, _) => {

    }
    case ReplicationDeleteFailure(group, _) => {
      logger.error(s"在分片中删除KV Group:[${group.id}]的数据错误。")
    }
    case DataDeleted(group, _) => {
      logger.error(s"Group ${group.id}已经删除了...")
    }
  }
}

/**
  * 事件定义
  */
object InMemoryKVService {

  case class GetKV(group: String, key: String)

  trait GetKVResult

  case class GetKVSuccess(key: String, value: Any) extends GetKVResult

  case class GetKVFailed(ex: Throwable) extends GetKVResult

  case class PutKV(group: String, key: String, value: Any)

  case class DeleteKVGroup(group: String)

  case class GroupDeleted(group: String) extends GetKVResult with PutKVResult

  trait PutKVResult

  case object PutKVSuccess extends PutKVResult

  case class PutKVFailed(key: String, ex: Throwable) extends PutKVResult

  case class Request(client: ActorRef, key: String)

}
