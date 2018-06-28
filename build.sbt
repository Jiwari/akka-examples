organization in ThisBuild := "com.github.jiwari"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.6"

lazy val root = Project("akka-examples", file("."))
  .settings(
    name := "akka-examples",
    libraryDependencies := commonDependencies
  )
  .aggregate(
    akkaAsk,
    akkaFSM,
    akkaPersistence,
    akkaState,
    akkaTell
  )

lazy val akkaAsk = Project("akka-ask", file("akka-ask"))
  .settings(
    name := "akka-ask",
    libraryDependencies := commonDependencies
  )

lazy val akkaFSM = Project("akka-fsm", file("akka-fsm"))
  .settings(
    name := "akka-fsm",
    libraryDependencies := commonDependencies
  )

lazy val akkaPersistence = Project("akka-persistence", file("akka-persistence"))
  .settings(
    name := "akka-persistence",
    libraryDependencies := commonDependencies ++ persistenceDependencies,
    resolvers += Resolver.jcenterRepo
  )

lazy val akkaPersistentFSM = Project("akka-persistent-fsm", file("akka-persistent-fsm"))
  .settings(
    name := "akka-persistent-fsm",
    libraryDependencies := commonDependencies ++ persistenceDependencies,
    resolvers += Resolver.jcenterRepo
  )

lazy val akkaState = Project("akka-state", file("akka-state"))
  .settings(
    name := "akka-state",
    libraryDependencies := commonDependencies
  )

lazy val akkaTell = Project("akka-tell", file("akka-tell"))
  .settings(
    name := "akka-tell",
    libraryDependencies := commonDependencies
  )

lazy val commonDependencies = Seq(
  "com.typesafe.akka"         %%  "akka-actor"        % "2.5.13",
  "com.typesafe.akka"         %%  "akka-testkit"      % "2.5.13"  % Test,
  "org.scalatest"             %%  "scalatest"         % "3.0.5"   % Test
)

lazy val persistenceDependencies = Seq(
  "com.typesafe.akka"         %%  "akka-persistence"          % "2.5.13",
  "com.github.dnvriend"       %% "akka-persistence-inmemory"  % "2.5.1.1"
)
