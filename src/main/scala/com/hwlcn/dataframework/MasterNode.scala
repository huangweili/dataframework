package com.hwlcn.dataframework

case class MasterNode(host: String, port: Int) {
  def toTuple: (String, Int) = {
    (host, port)
  }
}
