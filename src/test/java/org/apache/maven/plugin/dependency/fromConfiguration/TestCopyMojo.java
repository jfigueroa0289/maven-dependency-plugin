package org.apache.maven.plugin.dependency.fromConfiguration;

/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.AbstractDependencyMojoTestCase;
import org.apache.maven.plugin.dependency.testUtils.DependencyTestUtils;
import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.plugin.testing.stubs.StubArtifactCollector;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.plugin.testing.stubs.StubArtifactResolver;
import org.apache.maven.project.MavenProject;

public class TestCopyMojo
    extends AbstractDependencyMojoTestCase
{

    CopyMojo mojo;

    public TestCopyMojo()
    {
        super();
    }

    protected void setUp()
        throws Exception
    {
        super.setUp( "copy", false );

        File testPom = new File( getBasedir(), "target/test-classes/unit/copy-test/plugin-config.xml" );
        mojo = (CopyMojo) lookupMojo( "copy", testPom );
        mojo.setOutputDirectory( new File( this.testDir, "outputDirectory" ) );
        setSilent( mojo, true );

        assertNotNull( mojo );
        assertNotNull( mojo.getProject() );
        // MavenProject project = mojo.getProject();
        // init classifier things
        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( stubFactory, false, false ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );
        mojo.setArtifactCollector( new StubArtifactCollector() );

    }

    public ArtifactItem getSingleArtifactItem( boolean removeVersion, boolean useBaseVersion )
        throws MojoExecutionException
    {
        List<ArtifactItem> list = mojo.getProcessedArtifactItems(new ProcessArtifactItemsRequest( removeVersion, false, useBaseVersion, false ));
        return list.get( 0 );
    }

    public void testGetArtifactItems()
        throws MojoExecutionException
    {

        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifact" );
        item.setGroupId( "groupId" );
        item.setVersion( "1.0" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );

        mojo.setArtifactItems( list );

        ArtifactItem result = getSingleArtifactItem( false, false );
        assertEquals( mojo.getOutputDirectory(), result.getOutputDirectory() );

        File output = new File( mojo.getOutputDirectory(), "override" );
        item.setOutputDirectory( output );
        result = getSingleArtifactItem( false, false );
        assertEquals( output, result.getOutputDirectory() );
    }

    public void assertFilesExist( Collection<ArtifactItem> items, boolean exist )
    {
        for ( ArtifactItem item : items )
        {
            assertFileExists( item, exist );
        }
    }

    public void assertFileExists( ArtifactItem item, boolean exist )
    {
        File file = new File( item.getOutputDirectory(), item.getDestFileName() );
        assertEquals( exist, file.exists() );
    }

    public void testMojoDefaults()
    {
        CopyMojo themojo = new CopyMojo();

        assertFalse( themojo.isStripVersion() );
        assertFalse( themojo.isSkip() );
        assertFalse( themojo.isStripClassifier() );
    }

    public void testCopyFile()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertFilesExist( list, true );
    }
    
    public void testCopyFileWithBaseVersion()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifact" );
        item.setGroupId( "groupId" );
        item.setVersion( "1.0-20130210.213424-191" );
        item.setBaseVersion( "1.0-SNAPSHOT" );
        list.add(item);

        mojo.setArtifactItems( list );
        mojo.setUseBaseVersion(true);

        mojo.execute();

        assertFilesExist( list, true );
    }
    
    public void testSkip()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.setSkip( true );
        mojo.setArtifactItems( list );

        mojo.execute();
        for ( ArtifactItem item : list )
        {
            //these will be null because no processing has occured only when everything is skipped
            assertEquals( null, item.getOutputDirectory() );
            assertEquals( null, item.getDestFileName() );
        }

    }

    public void testCopyFileNoOverwrite()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        for ( ArtifactItem item : list )
        {
            // make sure that we copy even if false is set - MDEP-80
            item.setOverWrite( "false" );
        }

        mojo.setArtifactItems( list );
        mojo.execute();

        assertFilesExist( list, true );
    }

    public void testCopyToLocation()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = (ArtifactItem) list.get( 0 );
        item.setOutputDirectory( new File( mojo.getOutputDirectory(), "testOverride" ) );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertFilesExist( list, true );
    }

    public void testCopyStripVersionSetInMojo()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
        ArtifactItem item = (ArtifactItem) list.get( 0 );
        item.setOutputDirectory( new File( mojo.getOutputDirectory(), "testOverride" ) );
        mojo.setStripVersion( true );

        mojo.setArtifactItems( list );

        mojo.execute();
        assertEquals( DependencyUtil.getFormattedFileName( item.getArtifact(), true ), item.getDestFileName() );

        assertFilesExist( list, true );
    }

    public void testCopyStripClassifierSetInMojo()
            throws Exception
        {
            List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );
            ArtifactItem item = (ArtifactItem) list.get( 0 );
            item.setOutputDirectory( new File( mojo.getOutputDirectory(), "testOverride" ) );
            mojo.setStripClassifier( true );

            mojo.setArtifactItems( list );

            mojo.execute();
            assertEquals( DependencyUtil.getFormattedFileName( item.getArtifact(), false, false, false, true ), item.getDestFileName() );

            assertFilesExist( list, true );
        }
    
    public void testNonClassifierStrip()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getReleaseAndSnapshotArtifacts() );
        mojo.setStripVersion( true );
        mojo.setArtifactItems( list );

        mojo.execute();

        assertFilesExist( list, true );
    }

    public void testNonClassifierNoStrip()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getReleaseAndSnapshotArtifacts() );

        mojo.setArtifactItems( list );

        mojo.execute();

        assertFilesExist( list, true );
    }

    public void testMissingVersionNotFound()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        try
        {
            mojo.execute();
            fail( "Expected Exception Here." );
        }
        catch ( MojoExecutionException e )
        {
            // caught the expected exception.
        }
    }

    public List<Dependency> getDependencyList( ArtifactItem item )
    {
        Dependency dep = new Dependency();
        dep.setArtifactId( item.getArtifactId() );
        dep.setClassifier( item.getClassifier() );
        dep.setGroupId( item.getGroupId() );
        dep.setType( item.getType() );
        dep.setVersion( "2.0-SNAPSHOT" );

        Dependency dep2 = new Dependency();
        dep2.setArtifactId( item.getArtifactId() );
        dep2.setClassifier( "classifier" );
        dep2.setGroupId( item.getGroupId() );
        dep2.setType( item.getType() );
        dep2.setVersion( "2.1" );

        List<Dependency> list = new ArrayList<Dependency>( 2 );
        list.add( dep2 );
        list.add( dep );

        return list;
    }

    public void testMissingVersionFromDependencies()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        mojo.execute();
        this.assertFileExists( item, true );
        assertEquals( "2.0-SNAPSHOT", item.getVersion() );
    }

    public void testMissingVersionFromDependenciesLooseMatch()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        item.setClassifier( "sources" );
        item.setType( "jar" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.execute();
        this.assertFileExists( item, true );
        assertEquals( "2.1", item.getVersion() );
    }

    public void testMissingVersionFromDependenciesWithClassifier()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        mojo.execute();
        this.assertFileExists( item, true );
        assertEquals( "2.1", item.getVersion() );
    }

    public List<Dependency> getDependencyMgtList( ArtifactItem item )
    {
        Dependency dep = new Dependency();
        dep.setArtifactId( item.getArtifactId() );
        dep.setClassifier( item.getClassifier() );
        dep.setGroupId( item.getGroupId() );
        dep.setType( item.getType() );
        dep.setVersion( "3.0-SNAPSHOT" );

        Dependency dep2 = new Dependency();
        dep2.setArtifactId( item.getArtifactId() );
        dep2.setClassifier( "classifier" );
        dep2.setGroupId( item.getGroupId() );
        dep2.setType( item.getType() );
        dep2.setVersion( "3.1" );

        List<Dependency> list = new ArrayList<Dependency>( 2 );
        list.add( dep2 );
        list.add( dep );

        return list;
    }

    public void testMissingVersionFromDependencyMgt()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( getDependencyMgtList( item ) );

        mojo.execute();

        this.assertFileExists( item, true );
        assertEquals( "3.0-SNAPSHOT", item.getVersion() );
    }

    public void testMissingVersionFromDependencyMgtLooseMatch()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( getDependencyMgtList( item ) );

        item.setType( "jar" );
        mojo.execute();

        this.assertFileExists( item, true );
        assertEquals( "3.1", item.getVersion() );
    }

    public void testMissingVersionFromDependencyMgtWithClassifier()
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        MavenProject project = mojo.getProject();
        project.setDependencies( getDependencyList( item ) );

        item = new ArtifactItem();

        item.setArtifactId( "artifactId-2" );
        item.setClassifier( "classifier" );
        item.setGroupId( "groupId" );
        item.setType( "type" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );

        mojo.setArtifactItems( list );

        project.getDependencyManagement().setDependencies( getDependencyMgtList( item ) );

        mojo.execute();

        this.assertFileExists( item, true );
        assertEquals( "3.1", item.getVersion() );
    }

    public void testArtifactNotFound()
        throws Exception
    {
        dotestArtifactExceptions( false, true );
    }

    public void testArtifactResolutionException()
        throws Exception
    {
        dotestArtifactExceptions( true, false );
    }

    public void dotestArtifactExceptions( boolean are, boolean anfe )
        throws Exception
    {
        ArtifactItem item = new ArtifactItem();

        item.setArtifactId( "artifactId" );
        item.setClassifier( "" );
        item.setGroupId( "groupId" );
        item.setType( "type" );
        item.setVersion( "1.0" );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>();
        list.add( item );
        mojo.setArtifactItems( list );

        // init classifier things
        mojo.setFactory( DependencyTestUtils.getArtifactFactory() );
        mojo.setResolver( new StubArtifactResolver( null, are, anfe ) );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );

        try
        {
            mojo.execute();
            fail( "ExpectedException" );
        }
        catch ( MojoExecutionException e )
        {
            if ( are )
            {
                assertEquals( "Unable to resolve artifact.", e.getMessage() );
            }
            else
            {
                assertEquals( "Unable to find artifact.", e.getMessage() );
            }
        }
    }

    public void testNoArtifactItems()
    {
        try
        {
            mojo.getProcessedArtifactItems( new ProcessArtifactItemsRequest( false, false, false, false ) );
            fail( "Expected Exception" );
        }
        catch ( MojoExecutionException e )
        {
            assertEquals( "There are no artifactItems configured.", e.getMessage() );
        }

    }

    public void testCopyDontOverWriteReleases()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( release );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );

        mojo.execute();

        File copiedFile = new File( item.getOutputDirectory(), item.getDestFileName() );

        Thread.sleep( 100 );
        // round up to the next second
        long time = System.currentTimeMillis() + 1000;
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );
        Thread.sleep( 100 );

        mojo.execute();

        assertEquals( time, copiedFile.lastModified() );
    }

    public void testCopyDontOverWriteSnapshots()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );

        mojo.execute();

        File copiedFile = new File( item.getOutputDirectory(), item.getDestFileName() );

        Thread.sleep( 100 );
        // round up to the next second
        long time = System.currentTimeMillis() + 1000;
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );
        Thread.sleep( 100 );

        mojo.execute();

        assertEquals( time, copiedFile.lastModified() );
    }

    public void testCopyOverWriteReleases()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact release = stubFactory.getReleaseArtifact();
        release.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( release );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );
        mojo.setOverWriteReleases( true );
        mojo.execute();

        File copiedFile = new File( item.getOutputDirectory(), item.getDestFileName() );

        // round up to the next second
        long time = System.currentTimeMillis() - 2000;
        copiedFile.setLastModified( time );

        mojo.execute();

        assertTrue( time < copiedFile.lastModified() );
    }

    public void testCopyOverWriteSnapshot()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );

        mojo.setOverWriteIfNewer( false );
        mojo.setOverWriteReleases( false );
        mojo.setOverWriteSnapshots( true );
        mojo.execute();

        File copiedFile = new File( item.getOutputDirectory(), item.getDestFileName() );

        // round up to the next second
        long time = System.currentTimeMillis() - 2000;
        copiedFile.setLastModified( time );

        mojo.execute();

        assertTrue( time < copiedFile.lastModified() );
    }

    public void testCopyOverWriteIfNewer()
        throws Exception
    {
        stubFactory.setCreateFiles( true );
        Artifact artifact = stubFactory.getSnapshotArtifact();
        artifact.getFile().setLastModified( System.currentTimeMillis() - 2000 );

        ArtifactItem item = new ArtifactItem( artifact );

        List<ArtifactItem> list = new ArrayList<ArtifactItem>( 1 );
        list.add( item );
        mojo.setArtifactItems( list );
        mojo.setOverWriteIfNewer( true );
        mojo.execute();

        File copiedFile = new File( item.getOutputDirectory(), item.getDestFileName() );

        // set dest to be old
        long time = System.currentTimeMillis() - 10000;
        time = time - ( time % 1000 );
        copiedFile.setLastModified( time );

        // set source to be newer
        artifact.getFile().setLastModified( time + 4000 );
        mojo.execute();

        assertTrue( time < copiedFile.lastModified() );
    }
    
    public void testCopyFileWithOverideLocalRepo()
        throws Exception
    {
        List<ArtifactItem> list = stubFactory.getArtifactItems( stubFactory.getClassifiedArtifacts() );

        mojo.setArtifactItems( list );
        mojo.setLocal( new StubArtifactRepository( this.testDir.getAbsolutePath() ) );
        
        File execLocalRepo =  new File( this.testDir.getAbsolutePath(), "executionLocalRepo" );
        assertFalse( execLocalRepo.exists() );
        
        mojo.setLocalRepositoryDirectory( execLocalRepo );
        
        assertEquals( execLocalRepo.getAbsolutePath(), mojo.getLocal().getBasedir() ); 
        mojo.execute();

        assertFilesExist( list, true );
       
    }    

}
