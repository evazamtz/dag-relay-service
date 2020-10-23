package domain

case class Project(name:String, desc:String, token:String, fetchEndpoint:String, git:GitRepoSettings)
case class GitRepoSettings()

// TODO: status Model?
case class Resource(project:String, name:String, desc:String, payload:String)
case class DAGHistory() // ???
// admin
//

case class Deployment()

// ID?
// RAML for API apiary
//
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
// HttpClient
// htt
// Core Translator
// GitClient
// Storage
// Scheduler?
// Logger
// History?