/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/

package org.pentaho.platform.dataaccess.datasource.wizard.service.agile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.pentaho.platform.api.data.DBDatasourceServiceException;
import org.pentaho.platform.api.engine.IApplicationContext;
import org.pentaho.platform.api.engine.ICacheManager;
import org.pentaho.platform.api.engine.IPentahoObjectFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.ISystemSettings;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.engine.core.system.PentahoSystem;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgileHelperTest {

  private IPentahoObjectFactory pentahoObjectFactory;

  @Before
  public void setUp() throws SQLException, ObjectFactoryException {
    Connection connection = mock( Connection.class );
    DataSource dataSource = mock( DataSource.class );
    when( dataSource.getConnection() ).thenReturn( connection );

    final ICacheManager manager = mock( ICacheManager.class );
    when( manager.cacheEnabled( anyString() ) ).thenReturn( true );
    when( manager.getFromRegionCache( anyString(), any() ) ).thenReturn( dataSource );

    pentahoObjectFactory = mock( IPentahoObjectFactory.class );
    when( pentahoObjectFactory.objectDefined( anyString() ) ).thenReturn( true );
    when( pentahoObjectFactory.get( any(), anyString(), Mockito.<IPentahoSession>any() ) ).thenAnswer(
        new Answer<Object>() {
          @Override
          public Object answer( InvocationOnMock invocation ) throws Throwable {
            return manager;
          }
        } );
    PentahoSystem.registerObjectFactory( pentahoObjectFactory );

    IApplicationContext context = mock( IApplicationContext.class );
    when( context.getSolutionPath( anyString() ) ).thenAnswer( new Answer<String>() {
      @Override
      public String answer( InvocationOnMock invocation ) throws Throwable {
        return (String) invocation.getArguments()[0];
      }
    } );
    PentahoSystem.setApplicationContext( context );
  }

  @Test
  public void testGenerateTableName() {
    String sampleFilename = "test.Generate.Table.Name";
    String expectedFilename = "test_Generate_Table_Name";
    String actualFilename = AgileHelper.generateTableName( sampleFilename );
    assertTrue( expectedFilename.equals( actualFilename ) );
  }

  @Test
  public void testGetCsvSampleRowSize() {
    int defaultRowSize = 100;
    int expected = Integer.MAX_VALUE;

    PentahoSystem.setSystemSettingsService( null );
    assertEquals( defaultRowSize, AgileHelper.getCsvSampleRowSize() );

    ISystemSettings systemSettings = mock( ISystemSettings.class );
    when( systemSettings.getSystemSetting( anyString(), anyString(), any() ) ).thenReturn( null ).thenReturn(
        String.valueOf( expected ) );
    PentahoSystem.setSystemSettingsService( systemSettings );
    assertEquals( defaultRowSize, AgileHelper.getCsvSampleRowSize() );
    assertEquals( expected, AgileHelper.getCsvSampleRowSize() );
  }

  @Test
  public void testGetDatasourceSolutionStorage() {
    PentahoSystem.setSystemSettingsService( null );
    assertEquals( "admin", AgileHelper.getDatasourceSolutionStorage() );

    ISystemSettings systemSettings = mock( ISystemSettings.class );
    when( systemSettings.getSystemSetting( anyString(), anyString(), anyString() ) ).thenReturn( null );
    PentahoSystem.setSystemSettingsService( systemSettings );
    assertNull( AgileHelper.getDatasourceSolutionStorage() );
  }

  @Test
  public void testGetSchemaName() {
    PentahoSystem.setSystemSettingsService( null );
    assertNull( AgileHelper.getSchemaName() );

    ISystemSettings systemSettings = mock( ISystemSettings.class );
    when( systemSettings.getSystemSetting( anyString(), anyString(), anyString() ) ).thenReturn( null );
    PentahoSystem.setSystemSettingsService( systemSettings );
    assertNull( AgileHelper.getSchemaName() );
  }

  @Test
  public void testGetFolderPath() {
    PentahoSystem.setSystemSettingsService( null );
    assertNull( AgileHelper.getFolderPath( null ) );

    String sampleProject = AgileHelper.PLUGIN_NAME;
    String sampleFolderPath = "/etc/";
    ISystemSettings systemSettings = mock( ISystemSettings.class );
    when( systemSettings.getSystemSetting( anyString(), anyString(), any() ) ).thenReturn( null ).thenReturn(
        sampleFolderPath );
    PentahoSystem.setSystemSettingsService( systemSettings );

    assertNull( AgileHelper.getFolderPath( sampleProject ) );
    assertTrue( ( sampleFolderPath + sampleProject ).equals( AgileHelper.getFolderPath( sampleProject ) ) );
  }

  @Test
  public void testGetTmpFolderPath() {
    PentahoSystem.setSystemSettingsService( null );
    assertNull( AgileHelper.getTmpFolderPath( null ) );

    String sampleProject = AgileHelper.PLUGIN_NAME;
    String sampleFolderPath = "/etc/";
    ISystemSettings systemSettings = mock( ISystemSettings.class );
    when( systemSettings.getSystemSetting( anyString(), anyString(), any() ) )
      .thenReturn( null )
      .thenReturn( sampleFolderPath );
    PentahoSystem.setSystemSettingsService( systemSettings );

    assertNull( AgileHelper.getTmpFolderPath( sampleProject ) );
    assertTrue( ( sampleFolderPath + sampleProject ).equals( AgileHelper.getTmpFolderPath( sampleProject ) ) );
  }

  @Test
  public void testGetConnection() throws DBDatasourceServiceException, SQLException  {
    String jndiName = "HSQL";
    Connection connection =  AgileHelper.getConnection( jndiName );
    assertNotNull( connection );
  }
}
