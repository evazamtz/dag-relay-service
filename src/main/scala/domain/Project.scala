package domain

case class Project(name:String, token:String, fetchEndpoint:String, git:GitRepoSettings)
case class GitRepoSettings()
case class Dag(project:String, name:String, payload:String, status:Dag.Status)

object Dag {
  sealed trait Status
  case object Discovered  extends Status
  case object Synced      extends Status
  case object Desynced    extends Status
  case object Zombie      extends Status
}

// ID?
// RAML for API apiary
// list
// one
// all
// get scheduling
// OMG BRAH?



// если кто-то хочет сделать админку?
// apiary :D
// dags

// Modules
// Conf
// HttpClient (ProjectResourcesFetcher)?
// Core Translator
// Git
// Storage
// Scheduler?
// Logger
// History?


// API logic
// statuses of resources: lifecycle

// life cycle status:
// when does it get discovered first? from REPO or from API?
// ok brah<>
// []
// path to file<>
// unsynced = ?? deleted from API
// GIT <-> API
// was in GIT,.,

// scenarios: Was in API, has nothing in GIT
// scenarios: Both Version
// scenarios API->deleted -> deleted in GIT
// missing in API, was in GIT, but Has in Storage -><- ?
// all files in GIT -><-?