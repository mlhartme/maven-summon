/*
 * Copyright 1&1 Internet AG, https://github.com/1and1/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.oneandone.maven.summon.extension;

import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Repository;
import org.apache.maven.project.DefaultProjectBuildingHelper;
import org.apache.maven.project.ProjectBuildingHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Blocks defining repositories in poms. This is a security risk because attackers can inject code to be executed instead
 * of e.g. clean:clean and they can inject code to replace Maven components.
 *
 * Technically, repositories are not removed from the model, so they still show up in help:effective-pom.
 * Instead, repositories are removed from the repositories actually being used for dependency resolution.
 *
 * PomRepositoryBlocker not distinguish between normal and plugin repositories because they are usually not
 * distinguished when deploying, and they share the same local repository, so it's likely possible
 * to sneak plugin artifacts in by first resolving a normal artifact.
 */
@Component(role = ProjectBuildingHelper.class)
public class PomRepositoryBlocker extends DefaultProjectBuildingHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PomRepositoryBlocker.class);

    private final String logPrefix;
    private Restriction allowUrls;

    public PomRepositoryBlocker() {
        this.logPrefix = getClass().getSimpleName() + ": ";
        this.allowUrls = new Restriction().allowProperty(getClass().getName() + ":allow");
        LOGGER.info("{}created, allow {}", logPrefix, allowUrls);
    }

    public Restriction allowUrls() {
        return allowUrls;
    }

    /**
     * Builds the repositories used to resolve dependencies
     *
     * @param pomRepositories effective repositories in pom. CAUTION:
     *                        1) includes central, because that's in Maven's super pom
     *                        2) includes merged-in profiles from settings, i.e. it contains all external repositories
     *                        3) repos with same id are merged, i.e. central from super pom might have a different url coming from settings.
     * @param externalRepositories repos from settings
     * @param request configures merging in request.getRepositoryMerging (which maven sets to pom precedence)
     */
    @Override
    public List<ArtifactRepository> createArtifactRepositories(
            List<Repository> pomRepositories, List<ArtifactRepository> externalRepositories, ProjectBuildingRequest request) throws InvalidRepositoryException {
        List<ArtifactRepository> origResult;
        List<ArtifactRepository> filtered;

        origResult = super.createArtifactRepositories(pomRepositories, externalRepositories, request);
        filtered = new ArrayList<>();
        for (var repo : origResult) {
            if (containsUrl(externalRepositories, repo.getUrl())) {
                filtered.add(repo);
                LOGGER.info("{}external repository - ok: {}", logPrefix, repo.getUrl());
            } else if (allowUrls.isAllowed(repo.getUrl())) {
                filtered.add(repo);
                LOGGER.info("{}pom repository allowed: {}", logPrefix, repo.getUrl());
            } else {
                LOGGER.warn("{}pom repository blocked: {}", logPrefix, repo.getUrl());
            }
        }
        return filtered;
    }

    private static boolean containsUrl(List<ArtifactRepository> lst, String url) {
        for (var repo : lst) {
            if (url.equals(repo.getUrl())) {
                return true;
            }
        }
        return false;
    }
}
