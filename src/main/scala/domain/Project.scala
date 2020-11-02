package domain

case class Project(name:String, token:String, fetchEndpoint:String, git:GitRepoSettings)
case class GitRepoSettings()
case class Dag(project:String, name:String, payload:String)