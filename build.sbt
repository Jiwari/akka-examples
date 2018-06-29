organization in ThisBuild := "com.github.jiwari"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.12.6"

lazy val root = Project("akka-examples", file("."))
  .settings(
    libraryDependencies := commonDependencies
  )
  .aggregate(
    akkaAsk,
    akkaFSM,
    akkaPersistence,
    akkaPersistentFSM,
    akkaPersistentDelivery,
    akkaState,
    akkaTell
  )

lazy val akkaAsk = Project("akka-ask", file("akka-ask"))
  .settings(
    libraryDependencies := commonDependencies
  )

lazy val akkaFSM = Project("akka-fsm", file("akka-fsm"))
  .settings(
    libraryDependencies := commonDependencies
  )

lazy val akkaPersistence = Project("akka-persistence", file("akka-persistence"))
  .settings(
    libraryDependencies := commonDependencies ++ persistenceDependencies,
    resolvers += Resolver.jcenterRepo
  )

lazy val akkaPersistentFSM = Project("akka-persistent-fsm", file("akka-persistent-fsm"))
  .settings(
    libraryDependencies := commonDependencies ++ persistenceDependencies,
    resolvers += Resolver.jcenterRepo
  )

lazy val akkaPersistentDelivery = Project("akka-persistent-delivery", file("akka-persistent-delivery"))
  .settings(
    libraryDependencies := commonDependencies ++ persistenceDependencies,
    resolvers += Resolver.jcenterRepo
  )

lazy val akkaState = Project("akka-state", file("akka-state"))
  .settings(
    libraryDependencies := commonDependencies
  )

lazy val akkaTell = Project("akka-tell", file("akka-tell"))
  .settings(
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
