package object domain {
  type ProjectName = String
  type DagName = String
  type DagPayload = String

  case class Project(name:ProjectName, token:String, fetchEndpoint:String, git:GitRepoSettings)
  case class GitRepoSettings(repository: String, branch:String, path:String, privateToken:String)
  case class Dag(project:ProjectName, name:DagName, payload:DagPayload)
}
