package com.hwlcn.dataframework.master

case class MasterNode(host: String, port: Int) {
  def toTuple: (String, Int) = {
    (host, port)
  }
}
