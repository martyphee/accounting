package com.martyphee.accounting

import zio._

package object config {
  type DBConfigService = Has[DBConfig.Service]

  object DBConfig {
    trait Service {
      def host: String
      def port: Int
      def user: String
      def database: String
      def password: Option[String]
    }

    val live: Layer[Nothing, DBConfigService] = ZLayer.succeed(
      new DBConfig.Service {
        override def host: String = "localhost"
        override def port: Int = 5432
        override def user: String = "postgres"
        override def database: String = "world"
        override def password: Option[String] = None
      }
    )
  }

  //accessor methods
  def host: ZIO[DBConfigService, Throwable, String] = ZIO.access(_.get.host)
  def port: ZIO[DBConfigService, Throwable, Int] = ZIO.access(_.get.port)
  def user: ZIO[DBConfigService, Throwable, String] = ZIO.access(_.get.user)
  def database: ZIO[DBConfigService, Throwable, String] = ZIO.access(_.get.database)
  def password: ZIO[DBConfigService, Throwable, Option[String]] = ZIO.access(_.get.password)
}
