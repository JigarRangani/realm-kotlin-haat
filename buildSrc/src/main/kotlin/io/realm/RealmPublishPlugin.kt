/*
 * Copyright 2020 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.kotlin

import Realm
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

// Custom options for POM configurations that might differ between Realm modules
open class PomOptions {
    open var name: String = ""
    open var description: String = ""
}

// Configure how the Realm module is published
open class RealmPublishExtensions {
    open var pom: PomOptions = PomOptions()
    open fun pom(action: Action<PomOptions>) {
        action.execute(pom)
    }
}

// Helper function to get properties from gradle.properties or environment variables
fun getPropertyValue(project: Project, propertyName: String, defaultValue: String = ""): String {
    val property = project.findProperty(propertyName) as String?
    return property ?: System.getenv(propertyName) ?: defaultValue
}

// Plugin responsible for handling publishing to GitHub Packages
class RealmPublishPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = project.run {
        // We only apply publishing logic to sub-projects, not the root project.
        if (project != project.rootProject) {
            configureSubProject(project)
        }
    }

    private fun configureSubProject(project: Project) {
        with(project) {
            plugins.apply(MavenPublishPlugin::class.java)

            // Create the RealmPublish plugin extension.
            extensions.create<RealmPublishExtensions>("realmPublish")

            // Configure the POM details after the project has been evaluated
            afterEvaluate {
                project.extensions.findByType<RealmPublishExtensions>()?.run {
                    configurePom(project, pom)
                }
            }

            // Configure the publishing extension to point to GitHub Packages
            extensions.getByType<PublishingExtension>().apply {
                repositories {
                    maven {
                        name = "GitHubPackages"
                        // The URL for your GitHub Packages repository
                        // Make sure to replace YOUR_USERNAME and YOUR_REPOSITORY
                        url = uri("https://maven.pkg.github.com/${getPropertyValue(project, "gpr.user")}/${getPropertyValue(project, "gpr.repo")}")
                        credentials {
                            // Read credentials from gradle.properties
                            username = getPropertyValue(project, "gpr.user")
                            password = getPropertyValue(project, "gpr.key")
                        }
                    }
                }
            }
        }
    }

    private fun configurePom(project: Project, options: PomOptions) {
        project.extensions.getByType<PublishingExtension>().apply {
            publications.withType<MavenPublication>().all {
                // It's a good practice to modify the group ID to your own namespace
                groupId = getPropertyValue(project, "GROUP", "io.realm.kotlin")

                pom {
                    name.set(options.name)
                    description.set(options.description)
                    url.set(Realm.projectUrl)
                    licenses {
                        license {
                            name.set(Realm.License.name)
                            url.set(Realm.License.url)
                        }
                    }
                    issueManagement {
                        system.set(Realm.IssueManagement.system)
                        url.set(Realm.IssueManagement.url)
                    }
                    scm {
                        connection.set(Realm.SCM.connection)
                        developerConnection.set(Realm.SCM.developerConnection)
                        url.set(Realm.SCM.url)
                    }
                    developers {
                        developers {
                            developer {
                                // You can also update developer info if you wish
                                name.set(Realm.Developer.name)
                                email.set(Realm.Developer.email)
                                organization.set(Realm.Developer.organization)
                                organizationUrl.set(Realm.Developer.organizationUrl)
                            }
                        }
                    }
                }
            }
        }
    }
}