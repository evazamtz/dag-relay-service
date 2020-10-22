package domain

case class Project(name:String, desc:String, token:String, fetchEndpoint:String, git:GitRepoSettings)
case class GitRepoSettings()
case class Resource(project:String, name:String, desc:String, payload:String)
case class ResourceHistory()
case class Deployment()

