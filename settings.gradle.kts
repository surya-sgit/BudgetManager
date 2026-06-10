pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BudgetManager"

include(":app")
include(":core-common")
include(":core-ui")
include(":core-database")
include(":feature-dashboard")
include(":feature-transactions")
include(":feature-budget")
include(":feature-creditcards")
include(":feature-expensesplit")
include(":domain")
include(":data")
