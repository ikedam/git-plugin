/*
 * The MIT License
 * 
 * Copyright (c) 2016 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.git.extensions.impl;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import hudson.model.FreeStyleProject;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.plugins.git.SubmoduleConfig;
import hudson.plugins.git.TestGitRepo;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.plugins.git.UserRemoteConfig;
import hudson.plugins.git.extensions.GitSCMExtensionTest;
import hudson.plugins.git.extensions.GitSCMExtension;

import org.jenkinsci.plugins.gitclient.Git;
import org.jenkinsci.plugins.gitclient.GitClient;
import org.junit.Test;

/**
 * Tests for {@link CloneOption}
 */
public class CloneOptionTest extends GitSCMExtensionTest {
    protected CloneOption cloneOptionToTest;
    
    @Override
    protected void before() throws Exception {
    }
    
    @Override
    protected GitSCMExtension getExtension() {
        return cloneOptionToTest;
    }
    
    /**
     * Test whether the passed {@link CloneOption} is preserved
     * after configuration.
     * 
     * @param clone
     * @throws Exception
     */
    private void doTestConfigure(CloneOption clone) throws Exception {
        final String url = "https://github.com/jenkinsci/jenkins";
        FreeStyleProject p = j.createFreeStyleProject();
        GitSCM scm = new GitSCM(
                Arrays.asList(new UserRemoteConfig(url, "", "", "")),
                Arrays.asList(new BranchSpec("*/master")),
                false,  // doGenerateSubmoduleConfigurations
                Collections.<SubmoduleConfig>emptyList(),
                null,   // browser
                null,   // gitTool
                Arrays.<GitSCMExtension>asList(clone)
        );
        p.setScm(scm);
        j.configRoundtrip(p);
        j.assertEqualDataBoundBeans(scm, p.getScm());
        
        // Unfortunately, JenkinsRule#assertEqualDataBoundBeans
        // doesn't work for fields with @DataBoundStter for now.
        scm = (GitSCM)p.getScm();
        j.assertEqualDataBoundBeans(
                clone,
                scm.getExtensions().get(0)
        );
        assertEquals(
                clone.getDepth(),
                ((CloneOption)scm.getExtensions().get(0)).getDepth()
        );
    }
    
    @Test
    public void testConfigureNoShallow() throws Exception {
        CloneOption clone = new CloneOption(
                false,  // shallow,
                false,  // noTags,
                "",     // reference,
                null    // timeout
        );
        doTestConfigure(clone);
    }
    
    @Test
    public void testConfigureWithNoDepth() throws Exception {
        CloneOption clone = new CloneOption(
                true,   // shallow,
                false,  // noTags,
                "",     // reference,
                null    // timeout
        );
        clone.setDepth(null);
        doTestConfigure(clone);
    }
    
    @Test
    public void testConfigureWithDepth() throws Exception {
        CloneOption clone = new CloneOption(
                true,   // shallow,
                false,  // noTags,
                "",     // reference,
                null    // timeout
        );
        clone.setDepth(5);
        doTestConfigure(clone);
    }
    
    private TestGitRepo createRepoForShallowTest() throws Exception {
        TestGitRepo repo = new TestGitRepo("repo", tmp.newFolder(), listener);
        for (int i = 1; i <= 10; ++i) {
            repo.commit(
                    "afile",
                    Integer.toString(i),
                    repo.johnDoe,
                    String.format("Commit %d", i)
            );
        }
        return repo;
    }
    
    @Test
    public void testCloneWithoutDepth() throws Exception {
        cloneOptionToTest = new CloneOption(
                true,   // shallow,
                false,  // noTags,
                "",     // reference,
                null    // timeout
        );
        FreeStyleProject p = setupBasicProject(createRepoForShallowTest());
        FreeStyleBuild b = build(p, Result.SUCCESS);
        // jgit fails to handle a repository with only one commit.
        GitClient git = Git.with(listener, null).in(b.getWorkspace()).using("git").getClient();
        assertEquals(1, git.revList("HEAD").size());
    }
    
    @Test
    public void testCloneWithDepth() throws Exception {
        cloneOptionToTest = new CloneOption(
                true,   // shallow,
                false,  // noTags,
                "",     // reference,
                null    // timeout
        );
        cloneOptionToTest.setDepth(5);
        FreeStyleProject p = setupBasicProject(createRepoForShallowTest());
        FreeStyleBuild b = build(p, Result.SUCCESS);
        GitClient git = Git.with(listener, null).in(b.getWorkspace()).getClient();
        assertEquals(5, git.revList("HEAD").size());
    }
}
