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

package org.pentaho.platform.dataaccess.datasource.wizard.service.impl.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.commons.connection.IPentahoMetaData;
import org.pentaho.commons.connection.IPentahoResultSet;
import org.pentaho.commons.connection.marshal.MarshallableResultSet;
import org.pentaho.commons.connection.marshal.MarshallableRow;
import org.pentaho.database.DatabaseDialectException;
import org.pentaho.database.IDatabaseDialect;
import org.pentaho.database.dialect.GenericDatabaseDialect;
import org.pentaho.database.model.IDatabaseConnection;
import org.pentaho.database.service.DatabaseDialectService;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.dataaccess.datasource.beans.SerializedResultSet;
import org.pentaho.platform.dataaccess.datasource.wizard.service.ConnectionServiceException;
import org.pentaho.platform.dataaccess.datasource.wizard.service.DatasourceServiceException;
import org.pentaho.platform.dataaccess.datasource.wizard.service.impl.ConnectionServiceImpl;
import org.pentaho.platform.dataaccess.datasource.wizard.service.messages.Messages;
import org.pentaho.platform.plugin.services.connections.sql.SQLConnection;
import org.pentaho.platform.plugin.services.connections.sql.SQLMetaData;

public class DatasourceInMemoryServiceHelper {
  private static final Log logger = LogFactory.getLog( DatasourceInMemoryServiceHelper.class );

  /**
   * NOTE: caller is responsible for closing connection
   *
   * @param connectionName
   * @return
   * @throws DatasourceServiceException
   */
  public static java.sql.Connection getDataSourceConnection( String connectionName ) throws DatasourceServiceException {
    IDatabaseConnection connection = null;
    try {
      ConnectionServiceImpl service = new ConnectionServiceImpl();
      connection = service.getConnectionByName( connectionName );
    } catch ( ConnectionServiceException e1 ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0008_CONNECTION_SERVICE_EXCEPTION" ) ); //$NON-NLS-1$
      //we should return null because we do not able to use connection 
      return null;
    }
    java.sql.Connection conn = null;

    DatabaseDialectService dialectService = new DatabaseDialectService();
    IDatabaseDialect dialect = dialectService.getDialect( connection );
    String driverClass = null;
    if ( connection.getDatabaseType().getShortName().equals( "GENERIC" ) ) {
      driverClass = connection.getAttributes().get( GenericDatabaseDialect.ATTRIBUTE_CUSTOM_DRIVER_CLASS );
    } else {
      driverClass = dialect.getNativeDriver();
    }
    if ( StringUtils.isEmpty( driverClass ) ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0001_CONNECTION_ATTEMPT_FAILED" ) ); //$NON-NLS-1$
      throw new DatasourceServiceException( Messages
        .getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0001_CONNECTION_ATTEMPT_FAILED" ) ); //$NON-NLS-1$
    }
    Class<?> driverC = null;

    try {
      driverC = Class.forName( driverClass );
    } catch ( ClassNotFoundException e ) {
      logger.error( Messages
        .getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0002_DRIVER_NOT_FOUND_IN_CLASSPATH", driverClass ),
        e ); //$NON-NLS-1$
      throw new DatasourceServiceException(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0002_DRIVER_NOT_FOUND_IN_CLASSPATH" ),
        e ); //$NON-NLS-1$
    }
    if ( !Driver.class.isAssignableFrom( driverC ) ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0002_DRIVER_NOT_FOUND_IN_CLASSPATH",
        driverClass ) ); //$NON-NLS-1$
      throw new DatasourceServiceException( Messages
        .getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0002_DRIVER_NOT_FOUND_IN_CLASSPATH",
          driverClass ) ); //$NON-NLS-1$
    }
    Driver driver = null;

    try {
      driver = driverC.asSubclass( Driver.class ).newInstance();
    } catch ( InstantiationException e ) {
      logger.error(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0003_UNABLE_TO_INSTANCE_DRIVER", driverClass ),
        e ); //$NON-NLS-1$
      throw new DatasourceServiceException(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0003_UNABLE_TO_INSTANCE_DRIVER" ),
        e ); //$NON-NLS-1$
    } catch ( IllegalAccessException e ) {
      logger.error(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0003_UNABLE_TO_INSTANCE_DRIVER", driverClass ),
        e ); //$NON-NLS-1$
      throw new DatasourceServiceException(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0003_UNABLE_TO_INSTANCE_DRIVER" ),
        e ); //$NON-NLS-1$
    }
    try {
      DriverManager.registerDriver( driver );
      conn = DriverManager.getConnection( dialect.getURLWithExtraOptions( connection ), connection.getUsername(),
        connection.getPassword() );
      return conn;
    } catch ( SQLException e ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0004_UNABLE_TO_CONNECT" ),
        e ); //$NON-NLS-1$
      throw new DatasourceServiceException(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0004_UNABLE_TO_CONNECT" ), e ); //$NON-NLS-1$
    } catch ( DatabaseDialectException e ) {
      throw new DatasourceServiceException(
        Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0004_UNABLE_TO_CONNECT" ), e ); //$NON-NLS-1$
    }
  }

  public static SQLConnection getConnection( String connectionName ) throws DatasourceServiceException {
    IDatabaseConnection connection = null;
    try {
      ConnectionServiceImpl service = new ConnectionServiceImpl();
      connection = service.getConnectionByName( connectionName );
      DatabaseDialectService dialectService = new DatabaseDialectService();
      IDatabaseDialect dialect = dialectService.getDialect( connection );
      String driverClass = null;
      if ( connection.getDatabaseType().getShortName().equals( "GENERIC" ) ) {
        driverClass = connection.getAttributes().get( GenericDatabaseDialect.ATTRIBUTE_CUSTOM_DRIVER_CLASS );
      } else {
        driverClass = dialect.getNativeDriver();
      }

      return new SQLConnection( driverClass, dialect.getURLWithExtraOptions( connection ), connection.getUsername(),
        connection.getPassword(), null );
    } catch ( ConnectionServiceException e1 ) {
      return null;
    } catch ( DatabaseDialectException e ) {
      return null;
    }
  }

  public static SerializedResultSet getSerializeableResultSet( String connectionName, String query, int rowLimit,
                                                               IPentahoSession session )
    throws DatasourceServiceException {
    SerializedResultSet serializedResultSet = null;
    SQLConnection sqlConnection = null;
    try {
      sqlConnection = getConnection( connectionName );
      sqlConnection.setMaxRows( rowLimit );
      sqlConnection.setReadOnly( true );
      IPentahoResultSet resultSet = sqlConnection.executeQuery( query );
      MarshallableResultSet marshallableResultSet = new MarshallableResultSet();
      marshallableResultSet.setResultSet( resultSet );
      IPentahoMetaData ipmd = resultSet.getMetaData();
      if ( ipmd instanceof SQLMetaData ) {
        // Hack warning - get JDBC column types
        // TODO: Need to generalize this amongst all IPentahoResultSets
        SQLMetaData smd = (SQLMetaData) ipmd;
        int[] columnTypes = smd.getJDBCColumnTypes();
        List<List<String>> data = new ArrayList<List<String>>();
        for ( MarshallableRow row : marshallableResultSet.getRows() ) {
          String[] rowData = row.getCell();
          List<String> rowDataList = new ArrayList<String>( rowData.length );
          for ( int j = 0; j < rowData.length; j++ ) {
            rowDataList.add( rowData[ j ] );
          }
          data.add( rowDataList );
        }
        serializedResultSet =
          new SerializedResultSet( columnTypes, marshallableResultSet.getColumnNames().getColumnName(), data );
      }
    } catch ( Exception e ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0005_QUERY_VALIDATION_FAILED",
        e.getLocalizedMessage() ), e ); //$NON-NLS-1$
      throw new DatasourceServiceException( Messages
        .getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0005_QUERY_VALIDATION_FAILED",
          e.getLocalizedMessage() ), e ); //$NON-NLS-1$
    } finally {
      if ( sqlConnection != null ) {
        sqlConnection.close();
      }
    }

    return serializedResultSet;

  }


  public static List<List<String>> getCsvDataSample( String fileLocation, boolean headerPresent, String delimiter,
                                                     String enclosure, int rowLimit ) {
    String line = null;
    int row = 0;
    List<List<String>> dataSample = new ArrayList<List<String>>( rowLimit );
    File file = new File( fileLocation );
    BufferedReader bufRdr = null;
    try {
      bufRdr = new BufferedReader( new FileReader( file ) );
    } catch ( FileNotFoundException e ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0006_FILE_NOT_FOUND", fileLocation ), e );
    }
    //read each line of text file
    try {
      if ( bufRdr != null ) {
        while ( ( line = bufRdr.readLine() ) != null && row < rowLimit ) {
          StringTokenizer st = new StringTokenizer( line, delimiter );
          List<String> rowData = new ArrayList<String>();
          while ( st.hasMoreTokens() ) {
            //get next token and store it in the list
            rowData.add( st.nextToken() );
          }
          if ( headerPresent && row != 0 || !headerPresent ) {
            dataSample.add( rowData );
          }
          row++;
        }
        //close the file
        bufRdr.close();
      }
    } catch ( IOException e ) {
      logger.error( Messages.getErrorString( "DatasourceInMemoryServiceHelper.ERROR_0007_DO_NOT_HAVE_ACCESS_TO_FILE", fileLocation ), e );
    }
    return dataSample;
  }
}
