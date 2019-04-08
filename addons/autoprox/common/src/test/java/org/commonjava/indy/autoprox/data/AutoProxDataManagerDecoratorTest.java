/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.autoprox.data;

import static org.commonjava.indy.pkg.maven.model.MavenPackageTypeDescriptor.MAVEN_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.autoprox.conf.AutoProxConfig;
import org.commonjava.indy.autoprox.fixture.TestAutoProxFactory;
import org.commonjava.indy.autoprox.fixture.TestAutoProxyDataManager;
import org.commonjava.indy.autoprox.util.ScriptRuleParser;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.galley.auth.MemoryPasswordManager;
import org.commonjava.maven.galley.cache.FileCacheProvider;
import org.commonjava.maven.galley.event.NoOpFileEventManager;
import org.commonjava.maven.galley.io.HashedLocationPathGenerator;
import org.commonjava.maven.galley.io.NoOpTransferDecorator;
import org.commonjava.maven.galley.maven.GalleyMaven;
import org.commonjava.maven.galley.maven.GalleyMavenBuilder;
import org.commonjava.maven.galley.transport.htcli.HttpClientTransport;
import org.commonjava.maven.galley.transport.htcli.HttpImpl;
import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoProxDataManagerDecoratorTest
{

    public static final String REPO_ROOT_DIR = "repo.root.dir";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Rule
    public final ExpectationServer http = new ExpectationServer( "server-targets" );

    @Rule
    public final TestName name = new TestName();

    @Rule
    public final TemporaryFolder temp = new TemporaryFolder();

    private GalleyMaven galley;

    private AutoProxCatalogManager catalog;

    private StoreDataManager storeDataManager;

    private ArtifactStoreQuery<ArtifactStore> storeQuery;

    private ScriptRuleParser ruleParser;

    private final ChangeSummary summary = new ChangeSummary( "test-user", "test" );

    private File rootDir;

    private File autoproxDataDir;

    @Before
    public final void setup()
            throws Exception
    {
        rootDir = temp.newFolder( "indy.root" );
        autoproxDataDir = new File( rootDir, "data/autoprox" );
        autoproxDataDir.mkdirs();

        File cacheDir = temp.newFolder();
        FileCacheProvider cache =
                new FileCacheProvider( cacheDir, new HashedLocationPathGenerator(), new NoOpFileEventManager(),
                                       new NoOpTransferDecorator() );

        galley = new GalleyMavenBuilder( cache ).withEnabledTransports(
                new HttpClientTransport( new HttpImpl( new MemoryPasswordManager() ) ) ).build();

        final DataFileManager dataFiles = new DataFileManager( rootDir, new DataFileEventManager() );

        ruleParser = new ScriptRuleParser( new ScriptEngine( dataFiles ) );

        final AutoProxConfig indyConfig = new AutoProxConfig( autoproxDataDir.getName(), true );

        catalog = new AutoProxCatalogManager( dataFiles, indyConfig, ruleParser );
        storeDataManager = new TestAutoProxyDataManager( catalog, galley.getTransferManager() );

        storeDataManager.install();
        storeDataManager.clear( summary );

        storeQuery = storeDataManager.query().packageType( MAVEN_PKG_KEY );

        System.setProperty( "baseUrl", http.getBaseUri() );
    }

    @Test
    public void repositoryCreatedFromScannedDataDirRules()
            throws Exception
    {
        final URL u =
                Thread.currentThread().getContextClassLoader().getResource( "data/autoprox/simple-factory.groovy" );
        final File f = new File( u.getPath() );

        final File scriptFile = new File( autoproxDataDir, f.getName() );
        FileUtils.copyFile( f, scriptFile );

        System.out.println( "Parsing rules for: " + name.getMethodName() );
        catalog.parseRules();

        final String testUrl = http.formatUrl( "target", "test" );

        logger.info( "\n\nSETTING UP / VERIFYING REMOTE SERVER EXPECTATIONS" );
        http.expect( testUrl + "/", 200, "" );
        logger.info( "DONE: SETTING UP / VERIFYING REMOTE SERVER EXPECTATIONS\n\n" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getRemoteRepository( "test" ), nullValue() );
        catalog.setEnabled( true );

        final RemoteRepository repo = storeQuery.getRemoteRepository( "test" );

        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "test" ) );
        assertThat( repo.getUrl(), equalTo( testUrl ) );
    }

    @Test
    public void repositoryCreatedFromDeprecatedRule()
            throws Exception
    {
        final URL u =
                Thread.currentThread().getContextClassLoader().getResource( "data/autoprox/simple-deprecated-factory.groovy" );

        final File f = new File( u.getPath() );

        final File scriptFile = new File( autoproxDataDir, f.getName() );
        FileUtils.copyFile( f, scriptFile );

        System.out.println( "Parsing rules for: " + name.getMethodName() );
        catalog.parseRules();

        final String testUrl = http.formatUrl( "target", "test" );

        logger.info( "\n\nSETTING UP / VERIFYING REMOTE SERVER EXPECTATIONS" );
        http.expect( testUrl + "/", 200, "" );
        logger.info( "DONE: SETTING UP / VERIFYING REMOTE SERVER EXPECTATIONS\n\n" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getRemoteRepository( "test" ), nullValue() );
        catalog.setEnabled( true );

        final RemoteRepository repo = storeQuery.getRemoteRepository( "test" );

        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "test" ) );
        assertThat( repo.getUrl(), equalTo( testUrl ) );
    }

    @Test
    public void repositoryNOTCreatedFromScannedDataDirRulesWhenNameNotTest()
            throws Exception
    {
        final URL u =
                Thread.currentThread().getContextClassLoader().getResource( "data/autoprox/simple-factory.groovy" );
        final File f = new File( u.getPath() );
        final File scriptFile = new File( autoproxDataDir, f.getName() );
        FileUtils.copyFile( f, scriptFile );

        System.out.println( "Parsing rules for: " + name.getMethodName() );
        catalog.parseRules();

        final String testUrl = http.formatUrl( "target", "test" );
        http.expect( testUrl, 200, "" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getRemoteRepository( "foo" ), nullValue() );
        catalog.setEnabled( true );

        final RemoteRepository repo = storeQuery.getRemoteRepository( "foo" );

        assertThat( repo, nullValue() );
    }

    @Test
    public void repositoryAutoCreated()
            throws Exception
    {
        simpleCatalog();

        final String testUrl = http.formatUrl( "target", "test" );
        System.out.println( "Registering expectation of GET/HEAD to: " + testUrl + "/ with 200 response code." );
        http.expect( testUrl + "/", 200, "" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getRemoteRepository( "test" ), nullValue() );
        catalog.setEnabled( true );

        final RemoteRepository repo = storeQuery.getRemoteRepository( "test" );

        assertThat( repo, notNullValue() );
        assertThat( repo.getName(), equalTo( "test" ) );
        assertThat( repo.getUrl(), equalTo( testUrl ) );

    }

    private void simpleCatalog()
    {
        final TestAutoProxFactory fac = new TestAutoProxFactory( http );
        catalog.getRuleMappings().add( new RuleMapping( "test.groovy", null, fac ) );
    }

    @Test
    public void groupAutoCreatedWithDeployPointAndTwoRepos()
            throws Exception
    {
        simpleCatalog();

        final String testUrl = http.formatUrl( "target", "test" );
        http.expect( testUrl + "/", 200, "" );
        http.expect( http.formatUrl( "target", "first/" ), 200, "" );
        http.expect( http.formatUrl( "target", "second/" ), 200, "" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getGroup( "test" ), nullValue() );
        catalog.setEnabled( true );

        final Group group = storeQuery.getGroup( "test" );

        assertThat( group, notNullValue() );
        assertThat( group.getName(), equalTo( "test" ) );

        final List<StoreKey> constituents = group.getConstituents();

        logger.info( "Group constituents: {}", constituents );

        assertThat( constituents, notNullValue() );
        assertThat( constituents.size(), equalTo( 4 ) );

        int idx = 0;
        StoreKey key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.hosted ) );
        assertThat( key.getName(), equalTo( "test" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.remote ) );
        assertThat( key.getName(), equalTo( "test" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.remote ) );
        assertThat( key.getName(), equalTo( "first" ) );

        idx++;
        key = constituents.get( idx );

        assertThat( key.getType(), equalTo( StoreType.remote ) );
        assertThat( key.getName(), equalTo( "second" ) );
    }

    @Test
    public void repositoryNotAutoCreatedWhenTargetIsInvalid()
            throws Exception
    {
        simpleCatalog();

        final String testUrl = http.formatUrl( "target", "test" );
        http.expect( testUrl, 404, "" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getRemoteRepository( "test" ), nullValue() );
        catalog.setEnabled( true );

        final RemoteRepository repo = storeQuery.getRemoteRepository( "test" );

        assertThat( repo, nullValue() );

    }

    @Test
    public void groupNotAutoCreatedWhenTargetIsInvalid()
            throws Exception
    {
        simpleCatalog();

        final String testUrl = http.formatUrl( "target", "test" );
        http.expect( testUrl, 404, "" );

        catalog.setEnabled( false );
        assertThat( storeQuery.getGroup( "test" ), nullValue() );
        catalog.setEnabled( true );

        final Group group = storeQuery.getGroup( "test" );

        assertThat( group, nullValue() );
    }

}
