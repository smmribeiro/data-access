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


package org.pentaho.platform.dataaccess.datasource.wizard.service.impl;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.database.DatabaseDialectException;
import org.pentaho.database.IDatabaseDialect;
import org.pentaho.database.model.IDatabaseConnection;
import org.pentaho.database.service.DatabaseDialectService;
import org.pentaho.platform.dataaccess.datasource.wizard.service.ConnectionServiceException;
import org.pentaho.platform.dataaccess.datasource.wizard.service.gwt.IConnectionService;
import org.pentaho.platform.dataaccess.datasource.wizard.service.messages.Messages;

public class InMemoryConnectionServiceImpl implements IConnectionService {

  private List<IDatabaseConnection> connectionList = new ArrayList<IDatabaseConnection>();

  private static final Log logger = LogFactory.getLog( InMemoryConnectionServiceImpl.class );

  public InMemoryConnectionServiceImpl() {
  }

  public List<IDatabaseConnection> getConnections() throws ConnectionServiceException {
    return connectionList;
  }

  public IDatabaseConnection getConnectionByName( String name ) throws ConnectionServiceException {
    for ( IDatabaseConnection connection : connectionList ) {
      if ( connection.getName().equals( name ) ) {
        return connection;
      }
    }
    logger.error(
      Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0003_UNABLE_TO_GET_CONNECTION", name, null ) );
    throw new ConnectionServiceException(
      Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0003_UNABLE_TO_GET_CONNECTION", name, null ) );
  }

  public boolean addConnection( IDatabaseConnection connection ) throws ConnectionServiceException {
    if ( !isConnectionExist( connection.getName() ) ) {
      connectionList.add( connection );
      return true;
    } else {
      logger.error( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0004_UNABLE_TO_ADD_CONNECTION", connection.getName(),
          null ) );
      throw new ConnectionServiceException( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0004_UNABLE_TO_ADD_CONNECTION", connection.getName(),
          null ) );
    }
  }

  public boolean updateConnection( IDatabaseConnection connection ) throws ConnectionServiceException {
    IDatabaseConnection conn = getConnectionByName( connection.getName() );
    if ( conn != null ) {
      //      conn.setDriverClass(connection.getDriverClass());
      conn.setAccessType( connection.getAccessType() );
      conn.setPassword( connection.getPassword() );
      conn.setUsername( connection.getUsername() );
      return true;
    } else {
      logger.error( Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0005_UNABLE_TO_UPDATE_CONNECTION",
        connection.getName(), null ) );
      throw new ConnectionServiceException( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0005_UNABLE_TO_UPDATE_CONNECTION",
          connection.getName(), null ) );
    }
  }

  public boolean deleteConnection( IDatabaseConnection connection ) throws ConnectionServiceException {
    connectionList.remove( connectionList.indexOf( connection ) );
    return true;
  }

  public boolean deleteConnection( String name ) throws ConnectionServiceException {
    for ( IDatabaseConnection connection : connectionList ) {
      if ( connection.getName().equals( name ) ) {
        return deleteConnection( connection );
      }
    }
    logger.error( Messages
      .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0006_UNABLE_TO_DELETE_CONNECTION", name, null ) );
    throw new ConnectionServiceException( Messages
      .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0006_UNABLE_TO_DELETE_CONNECTION", name, null ) );
  }

  public boolean testConnection( IDatabaseConnection connection ) throws ConnectionServiceException {
    java.sql.Connection conn = null;
    try {
      conn = getConnection( connection );
    } catch ( ConnectionServiceException dme ) {
      logger.error( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0026_UNABLE_TO_TEST_CONNECTION", connection.getName(),
          dme.getLocalizedMessage() ), dme );
      throw new ConnectionServiceException( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0026_UNABLE_TO_TEST_CONNECTION", connection.getName(),
          dme.getLocalizedMessage() ), dme );
    } finally {
      try {
        if ( conn != null ) {
          conn.close();
        }
      } catch ( SQLException e ) {
        logger.error( Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0026_UNABLE_TO_TEST_CONNECTION",
          connection.getName(), null ) );
        throw new ConnectionServiceException( Messages
          .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0026_UNABLE_TO_TEST_CONNECTION",
            connection.getName(), null ) );
      }
    }
    return true;
  }

  /**
   * NOTE: caller is responsible for closing connection
   *
   * @param ds
   * @return
   * @throws DataSourceManagementException
   */
  private static java.sql.Connection getConnection( IDatabaseConnection connection ) throws ConnectionServiceException {
    java.sql.Connection conn = null;

    String driverClass = connection.getAccessType().getClass().toString();
    if ( StringUtils.isEmpty( driverClass ) ) {
      logger
        .error( Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0020_CONNECTION_ATTEMPT_FAILED" ) );
      throw new ConnectionServiceException( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0020_CONNECTION_ATTEMPT_FAILED" ) ); //$NON-NLS-1$

    }
    Class<?> driverC = null;

    try {
      driverC = Class.forName( driverClass );
    } catch ( ClassNotFoundException e ) {
      logger.error( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0021_DRIVER_NOT_FOUND_IN_CLASSPATH", driverClass ),
        e );
      throw new ConnectionServiceException(
        Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0021_DRIVER_NOT_FOUND_IN_CLASSPATH" ),
        e ); //$NON-NLS-1$

    }
    if ( !Driver.class.isAssignableFrom( driverC ) ) {
      logger.error( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0021_DRIVER_NOT_FOUND_IN_CLASSPATH", driverClass ) );
      throw new ConnectionServiceException( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0021_DRIVER_NOT_FOUND_IN_CLASSPATH" ) ); //$NON-NLS-1$

    }
    Driver driver = null;

    try {
      driver = driverC.asSubclass( Driver.class ).newInstance();
    } catch ( InstantiationException e ) {
      logger.error( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0022_UNABLE_TO_INSTANCE_DRIVER", driverClass ), e );
      throw new ConnectionServiceException(
        Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0022_UNABLE_TO_INSTANCE_DRIVER" ),
        e ); //$NON-NLS-1$
    } catch ( IllegalAccessException e ) {
      logger.error( Messages
        .getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0022_UNABLE_TO_INSTANCE_DRIVER", driverClass ), e );
      throw new ConnectionServiceException(
        Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0022_UNABLE_TO_INSTANCE_DRIVER" ),
        e ); //$NON-NLS-1$
    }
    try {
      DriverManager.registerDriver( driver );
      DatabaseDialectService dialectService = new DatabaseDialectService();
      IDatabaseDialect dialect = dialectService.getDialect( connection );

      conn = DriverManager.getConnection( dialect.getURLWithExtraOptions( connection ), connection.getUsername(),
        connection.getPassword() );
      return conn;
    } catch ( SQLException e ) {
      logger.error( Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0023_UNABLE_TO_CONNECT" ), e );
      throw new ConnectionServiceException(
        Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0023_UNABLE_TO_CONNECT" ), e ); //$NON-NLS-1$
    } catch ( DatabaseDialectException e ) {
      logger.error( Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0023_UNABLE_TO_CONNECT" ), e );
      throw new ConnectionServiceException(
        Messages.getErrorString( "ConnectionServiceInMemoryDelegate.ERROR_0023_UNABLE_TO_CONNECT" ), e ); //$NON-NLS-1$
    }
  }

  public boolean isConnectionExist( String connectionName ) {
    for ( IDatabaseConnection connection : connectionList ) {
      if ( connection.getName().equals( connectionName ) ) {
        return true;
      }
    }
    return false;
  }
}
