/*
 * DISCLAIMER
 *
 * Copyright 2016 ArangoDB GmbH, Cologne, Germany
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
 *
 * Copyright holder is ArangoDB GmbH, Cologne, Germany
 */

package com.arangodb;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.arangodb.entity.AqlExecutionExplainEntity;
import com.arangodb.entity.AqlFunctionEntity;
import com.arangodb.entity.AqlParseEntity;
import com.arangodb.entity.ArangoDBVersion;
import com.arangodb.entity.CollectionEntity;
import com.arangodb.entity.CursorEntity;
import com.arangodb.entity.DatabaseEntity;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.IndexEntity;
import com.arangodb.entity.Permissions;
import com.arangodb.entity.QueryCachePropertiesEntity;
import com.arangodb.entity.QueryEntity;
import com.arangodb.entity.QueryTrackingPropertiesEntity;
import com.arangodb.entity.TraversalEntity;
import com.arangodb.internal.ArangoCursorExecute;
import com.arangodb.internal.ArangoExecutorAsync;
import com.arangodb.internal.CollectionCache;
import com.arangodb.internal.DocumentCache;
import com.arangodb.internal.InternalArangoDatabase;
import com.arangodb.internal.net.HostHandle;
import com.arangodb.internal.velocystream.ConnectionAsync;
import com.arangodb.internal.velocystream.VstCommunicationAsync;
import com.arangodb.model.AqlFunctionCreateOptions;
import com.arangodb.model.AqlFunctionDeleteOptions;
import com.arangodb.model.AqlFunctionGetOptions;
import com.arangodb.model.AqlQueryExplainOptions;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.model.CollectionCreateOptions;
import com.arangodb.model.CollectionsReadOptions;
import com.arangodb.model.DocumentReadOptions;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.model.TransactionOptions;
import com.arangodb.model.TraversalOptions;
import com.arangodb.util.ArangoSerialization;
import com.arangodb.velocypack.Type;
import com.arangodb.velocystream.Request;
import com.arangodb.velocystream.Response;

/**
 * @author Mark Vollmary
 *
 */
public class ArangoDatabaseAsync extends
		InternalArangoDatabase<ArangoDBAsync, ArangoExecutorAsync, CompletableFuture<Response>, ConnectionAsync> {

	protected ArangoDatabaseAsync(final ArangoDBAsync arangoDB, final String name) {
		super(arangoDB, arangoDB.executor(), arangoDB.util(), name);
	}

	protected ArangoDatabaseAsync(final VstCommunicationAsync communication, final ArangoSerialization util,
		final DocumentCache documentCache, final CollectionCache collectionCache, final String name) {
		super(null, new ArangoExecutorAsync(communication, util, documentCache), util, name);
	}

	/**
	 * Returns the server name and version number.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/MiscellaneousFunctions/index.html#return-server-version">API
	 *      Documentation</a>
	 * @return the server version, number
	 */
	public CompletableFuture<ArangoDBVersion> getVersion() {
		return executor.execute(getVersionRequest(), ArangoDBVersion.class);
	}

	/**
	 * Checks whether the database exists
	 * 
	 * @return true if the database exists, otherwise false
	 */
	public CompletableFuture<Boolean> exists() {
		return getInfo().thenApply(info -> info != null).exceptionally(e -> e == null);
	}

	/**
	 * Retrieves a list of all databases the current user can access
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#list-of-accessible-databases">API
	 *      Documentation</a>
	 * @return a list of all databases the current user can access
	 */
	public CompletableFuture<Collection<String>> getAccessibleDatabases() {
		return executor.execute(getAccessibleDatabasesRequest(), getDatabaseResponseDeserializer());
	}

	/**
	 * Returns a handler of the collection by the given name
	 * 
	 * @param name
	 *            Name of the collection
	 * @return collection handler
	 */
	public ArangoCollectionAsync collection(final String name) {
		return new ArangoCollectionAsync(this, name);
	}

	/**
	 * Creates a collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#create-collection">API
	 *      Documentation</a>
	 * @param name
	 *            The name of the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> createCollection(final String name) {
		return executor.execute(createCollectionRequest(name, new CollectionCreateOptions()), CollectionEntity.class);
	}

	/**
	 * Creates a collection
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Creating.html#create-collection">API
	 *      Documentation</a>
	 * @param name
	 *            The name of the collection
	 * @param options
	 *            Additional options, can be null
	 * @return information about the collection
	 */
	public CompletableFuture<CollectionEntity> createCollection(
		final String name,
		final CollectionCreateOptions options) {
		return executor.execute(createCollectionRequest(name, options), CollectionEntity.class);
	}

	/**
	 * Returns all collections
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Getting.html#reads-all-collections">API
	 *      Documentation</a>
	 * @return list of information about all collections
	 */
	public CompletableFuture<Collection<CollectionEntity>> getCollections() {
		return executor.execute(getCollectionsRequest(new CollectionsReadOptions()),
			getCollectionsResponseDeserializer());
	}

	/**
	 * Returns all collections
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Collection/Getting.html#reads-all-collections">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return list of information about all collections
	 */
	public CompletableFuture<Collection<CollectionEntity>> getCollections(final CollectionsReadOptions options) {
		return executor.execute(getCollectionsRequest(options), getCollectionsResponseDeserializer());
	}

	/**
	 * Returns an index
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/WorkingWith.html#read-index">API Documentation</a>
	 * @param id
	 *            The index-handle
	 * @return information about the index
	 */
	public CompletableFuture<IndexEntity> getIndex(final String id) {
		executor.validateIndexId(id);
		final String[] split = id.split("/");
		return collection(split[0]).getIndex(split[1]);
	}

	/**
	 * Deletes an index
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Indexes/WorkingWith.html#delete-index">API Documentation</a>
	 * @param id
	 *            The index handle
	 * @return the id of the index
	 */
	public CompletableFuture<String> deleteIndex(final String id) {
		executor.validateIndexId(id);
		final String[] split = id.split("/");
		return collection(split[0]).deleteIndex(split[1]);
	}

	/**
	 * Drop an existing database
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#drop-database">API
	 *      Documentation</a>
	 * @return true if the database was dropped successfully
	 */
	public CompletableFuture<Boolean> drop() {
		return executor.execute(dropRequest(), createDropResponseDeserializer());
	}

	/**
	 * Grants access to the database dbname for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @param permissions
	 *            The permissions the user grant
	 * @return void
	 */
	public CompletableFuture<Void> grantAccess(final String user, final Permissions permissions) {
		return executor.execute(grantAccessRequest(user, permissions), Void.class);
	}

	/**
	 * Grants access to the database dbname for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return void
	 */
	public CompletableFuture<Void> grantAccess(final String user) {
		return executor.execute(grantAccessRequest(user, Permissions.RW), Void.class);
	}

	/**
	 * Revokes access to the database dbname for user user. You need permission to the _system database in order to
	 * execute this call.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return void
	 */
	public CompletableFuture<Void> revokeAccess(final String user) {
		return executor.execute(grantAccessRequest(user, Permissions.NONE), Void.class);
	}

	/**
	 * Clear the database access level, revert back to the default access level.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/index.html#grant-or-revoke-database-access">
	 *      API Documentation</a>
	 * @param user
	 *            The name of the user
	 * @since ArangoDB 3.2.0
	 * @return void
	 */
	public CompletableFuture<Void> resetAccess(final String user) {
		return executor.execute(resetAccessRequest(user), Void.class);
	}

	/**
	 * Sets the default access level for collections within this database for the user <code>user</code>. You need
	 * permission to the _system database in order to execute this call.
	 * 
	 * @param user
	 *            The name of the user
	 * @param permissions
	 *            The permissions the user grant
	 * @since ArangoDB 3.2.0
	 * @throws ArangoDBException
	 */
	public CompletableFuture<Void> grantDefaultCollectionAccess(final String user, final Permissions permissions)
			throws ArangoDBException {
		return executor.execute(updateUserDefaultCollectionAccessRequest(user, permissions), Void.class);
	}

	/**
	 * @deprecated use {@link #grantDefaultCollectionAccess(String, Permissions)} instead
	 * @param user
	 *            The name of the user
	 * @param permissions
	 *            The permissions the user grant
	 * @since ArangoDB 3.2.0
	 */
	@Deprecated
	public CompletableFuture<Void> updateUserDefaultCollectionAccess(final String user, final Permissions permissions) {
		return executor.execute(updateUserDefaultCollectionAccessRequest(user, permissions), Void.class);
	}

	/**
	 * Get specific database access level
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/UserManagement/#get-the-database-access-level"> API
	 *      Documentation</a>
	 * @param user
	 *            The name of the user
	 * @return permissions of the user
	 * @since ArangoDB 3.2.0
	 */
	public CompletableFuture<Permissions> getPermissions(final String user) throws ArangoDBException {
		return executor.execute(getPermissionsRequest(user), getPermissionsResponseDeserialzer());
	}

	/**
	 * Create a cursor and return the first results
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#create-cursor">API
	 *      Documentation</a>
	 * @param query
	 *            contains the query string to be executed
	 * @param bindVars
	 *            key/value pairs representing the bind parameters
	 * @param options
	 *            Additional options, can be null
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 * @throws ArangoDBException
	 */
	public <T> CompletableFuture<ArangoCursorAsync<T>> query(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryOptions options,
		final Class<T> type) throws ArangoDBException {
		final Request request = queryRequest(query, bindVars, options);
		final CompletableFuture<CursorEntity> execution = executor.execute(request, CursorEntity.class);
		return execution.thenApply(result -> {
			return createCursor(result, type);
		});
	}

	/**
	 * Return an cursor from the given cursor-ID if still existing
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCursor/AccessingCursors.html#read-next-batch-from-cursor">API
	 *      Documentation</a>
	 * @param cursorId
	 *            The ID of the cursor
	 * @param type
	 *            The type of the result (POJO class, VPackSlice, String for Json, or Collection/List/Map)
	 * @return cursor of the results
	 * @throws ArangoDBException
	 */
	public <T> CompletableFuture<ArangoCursorAsync<T>> cursor(final String cursorId, final Class<T> type)
			throws ArangoDBException {
		final CompletableFuture<CursorEntity> execution = executor.execute(queryNextRequest(cursorId),
			CursorEntity.class);
		return execution.thenApply(result -> {
			return createCursor(result, type);
		});
	}

	private <T> ArangoCursorAsync<T> createCursor(final CursorEntity result, final Class<T> type) {
		return new ArangoCursorAsync<>(this, new ArangoCursorExecute() {
			@Override
			public CursorEntity next(final String id, final HostHandle hostHandle) {
				final CompletableFuture<CursorEntity> result = executor.execute(queryNextRequest(id),
					CursorEntity.class, hostHandle);
				try {
					return result.get();
				} catch (InterruptedException | ExecutionException e) {
					throw new ArangoDBException(e);
				}
			}

			@Override
			public void close(final String id, final HostHandle hostHandle) {
				try {
					executor.execute(queryCloseRequest(id), Void.class, hostHandle).get();
				} catch (InterruptedException | ExecutionException e) {
					throw new ArangoDBException(e);
				}
			}
		}, type, result);
	}

	/**
	 * Explain an AQL query and return information about it
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#explain-an-aql-query">API
	 *      Documentation</a>
	 * @param query
	 *            the query which you want explained
	 * @param bindVars
	 *            key/value pairs representing the bind parameters
	 * @param options
	 *            Additional options, can be null
	 * @return information about the query
	 */
	public CompletableFuture<AqlExecutionExplainEntity> explainQuery(
		final String query,
		final Map<String, Object> bindVars,
		final AqlQueryExplainOptions options) {
		return executor.execute(explainQueryRequest(query, bindVars, options), AqlExecutionExplainEntity.class);
	}

	/**
	 * Parse an AQL query and return information about it This method is for query validation only. To actually query
	 * the database, see {@link ArangoDatabaseAsync#query(String, Map, AqlQueryOptions, Class)}
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#parse-an-aql-query">API
	 *      Documentation</a>
	 * @param query
	 *            the query which you want parse
	 * @return imformation about the query
	 */
	public CompletableFuture<AqlParseEntity> parseQuery(final String query) {
		return executor.execute(parseQueryRequest(query), AqlParseEntity.class);
	}

	/**
	 * Clears the AQL query cache
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCache/index.html#clears-any-results-in-the-aql-query-cache">API
	 *      Documentation</a>
	 * @return void
	 */
	public CompletableFuture<Void> clearQueryCache() {
		return executor.execute(clearQueryCacheRequest(), Void.class);
	}

	/**
	 * Returns the global configuration for the AQL query cache
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCache/index.html#returns-the-global-properties-for-the-aql-query-cache">API
	 *      Documentation</a>
	 * @return configuration for the AQL query cache
	 */
	public CompletableFuture<QueryCachePropertiesEntity> getQueryCacheProperties() {
		return executor.execute(getQueryCachePropertiesRequest(), QueryCachePropertiesEntity.class);
	}

	/**
	 * Changes the configuration for the AQL query cache. Note: changing the properties may invalidate all results in
	 * the cache.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQueryCache/index.html#globally-adjusts-the-aql-query-result-cache-properties">API
	 *      Documentation</a>
	 * @param properties
	 *            properties to be set
	 * @return current set of properties
	 */
	public CompletableFuture<QueryCachePropertiesEntity> setQueryCacheProperties(
		final QueryCachePropertiesEntity properties) {
		return executor.execute(setQueryCachePropertiesRequest(properties), QueryCachePropertiesEntity.class);
	}

	/**
	 * Returns the configuration for the AQL query tracking
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#returns-the-properties-for-the-aql-query-tracking">API
	 *      Documentation</a>
	 * @return configuration for the AQL query tracking
	 */
	public CompletableFuture<QueryTrackingPropertiesEntity> getQueryTrackingProperties() {
		return executor.execute(getQueryTrackingPropertiesRequest(), QueryTrackingPropertiesEntity.class);
	}

	/**
	 * Changes the configuration for the AQL query tracking
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#changes-the-properties-for-the-aql-query-tracking">API
	 *      Documentation</a>
	 * @param properties
	 *            properties to be set
	 * @return current set of properties
	 */
	public CompletableFuture<QueryTrackingPropertiesEntity> setQueryTrackingProperties(
		final QueryTrackingPropertiesEntity properties) {
		return executor.execute(setQueryTrackingPropertiesRequest(properties), QueryTrackingPropertiesEntity.class);
	}

	/**
	 * Returns a list of currently running AQL queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#returns-the-currently-running-aql-queries">API
	 *      Documentation</a>
	 * @return a list of currently running AQL queries
	 */
	public CompletableFuture<Collection<QueryEntity>> getCurrentlyRunningQueries() {
		return executor.execute(getCurrentlyRunningQueriesRequest(), new Type<Collection<QueryEntity>>() {
		}.getType());
	}

	/**
	 * Returns a list of slow running AQL queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#returns-the-list-of-slow-aql-queries">API
	 *      Documentation</a>
	 * @return a list of slow running AQL queries
	 */
	public CompletableFuture<Collection<QueryEntity>> getSlowQueries() {
		return executor.execute(getSlowQueriesRequest(), new Type<Collection<QueryEntity>>() {
		}.getType());
	}

	/**
	 * Clears the list of slow AQL queries
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#clears-the-list-of-slow-aql-queries">API
	 *      Documentation</a>
	 * @return void
	 */
	public CompletableFuture<Void> clearSlowQueries() {
		return executor.execute(clearSlowQueriesRequest(), Void.class);
	}

	/**
	 * Kills a running query. The query will be terminated at the next cancelation point.
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/AqlQuery/index.html#kills-a-running-aql-query">API
	 *      Documentation</a>
	 * @param id
	 *            The id of the query
	 * @return void
	 */
	public CompletableFuture<Void> killQuery(final String id) {
		return executor.execute(killQueryRequest(id), Void.class);
	}

	/**
	 * Create a new AQL user function
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/AqlUserFunctions/index.html#create-aql-user-function">API
	 *      Documentation</a>
	 * @param name
	 *            the fully qualified name of the user functions
	 * @param code
	 *            a string representation of the function body
	 * @param options
	 *            Additional options, can be null
	 * @return void
	 */
	public CompletableFuture<Void> createAqlFunction(
		final String name,
		final String code,
		final AqlFunctionCreateOptions options) {
		return executor.execute(createAqlFunctionRequest(name, code, options), Void.class);

	}

	/**
	 * Remove an existing AQL user function
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlUserFunctions/index.html#remove-existing-aql-user-function">API
	 *      Documentation</a>
	 * @param name
	 *            the name of the AQL user function
	 * @param options
	 *            Additional options, can be null
	 * @return void
	 */
	public CompletableFuture<Void> deleteAqlFunction(final String name, final AqlFunctionDeleteOptions options) {
		return executor.execute(deleteAqlFunctionRequest(name, options), Void.class);
	}

	/**
	 * Gets all reqistered AQL user functions
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AqlUserFunctions/index.html#return-registered-aql-user-functions">API
	 *      Documentation</a>
	 * @param options
	 *            Additional options, can be null
	 * @return all reqistered AQL user functions
	 */
	public CompletableFuture<Collection<AqlFunctionEntity>> getAqlFunctions(final AqlFunctionGetOptions options) {
		return executor.execute(getAqlFunctionsRequest(options), new Type<Collection<AqlFunctionEntity>>() {
		}.getType());
	}

	/**
	 * Returns a handler of the graph by the given name
	 * 
	 * @param name
	 *            Name of the graph
	 * @return graph handler
	 */
	public ArangoGraphAsync graph(final String name) {
		return new ArangoGraphAsync(this, name);
	}

	/**
	 * Create a new graph in the graph module. The creation of a graph requires the name of the graph and a definition
	 * of its edges.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#create-a-graph">API
	 *      Documentation</a>
	 * @param name
	 *            Name of the graph
	 * @param edgeDefinitions
	 *            An array of definitions for the edge
	 * @return information about the graph
	 */
	public CompletableFuture<GraphEntity> createGraph(
		final String name,
		final Collection<EdgeDefinition> edgeDefinitions) {
		return executor.execute(createGraphRequest(name, edgeDefinitions, new GraphCreateOptions()),
			createGraphResponseDeserializer());
	}

	/**
	 * Create a new graph in the graph module. The creation of a graph requires the name of the graph and a definition
	 * of its edges.
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#create-a-graph">API
	 *      Documentation</a>
	 * @param name
	 *            Name of the graph
	 * @param edgeDefinitions
	 *            An array of definitions for the edge
	 * @param options
	 *            Additional options, can be null
	 * @return information about the graph
	 */
	public CompletableFuture<GraphEntity> createGraph(
		final String name,
		final Collection<EdgeDefinition> edgeDefinitions,
		final GraphCreateOptions options) {
		return executor.execute(createGraphRequest(name, edgeDefinitions, options), createGraphResponseDeserializer());
	}

	/**
	 * Lists all graphs known to the graph module
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Gharial/Management.html#list-all-graphs">API
	 *      Documentation</a>
	 * @return graphs stored in this database
	 */
	public CompletableFuture<Collection<GraphEntity>> getGraphs() {
		return executor.execute(getGraphsRequest(), getGraphsResponseDeserializer());
	}

	/**
	 * Execute a server-side transaction
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Transaction/index.html#execute-transaction">API
	 *      Documentation</a>
	 * @param action
	 *            the actual transaction operations to be executed, in the form of stringified JavaScript code
	 * @param type
	 *            The type of the result (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return the result of the transaction if it succeeded
	 */
	public <T> CompletableFuture<T> transaction(
		final String action,
		final Class<T> type,
		final TransactionOptions options) {
		return executor.execute(transactionRequest(action, options), transactionResponseDeserializer(type));
	}

	/**
	 * Retrieves information about the current database
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/Database/DatabaseManagement.html#information-of-the-database">API
	 *      Documentation</a>
	 * @return information about the current database
	 */
	public CompletableFuture<DatabaseEntity> getInfo() {
		return executor.execute(getInfoRequest(), getInfoResponseDeserializer());
	}

	/**
	 * Execute a server-side traversal
	 * 
	 * @see <a href= "https://docs.arangodb.com/current/HTTP/Traversal/index.html#executes-a-traversal">API
	 *      Documentation</a>
	 * @param vertexClass
	 *            The type of the vertex documents (POJO class, VPackSlice or String for Json)
	 * @param edgeClass
	 *            The type of the edge documents (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options
	 * @return Result of the executed traversal
	 */
	public <V, E> CompletableFuture<TraversalEntity<V, E>> executeTraversal(
		final Class<V> vertexClass,
		final Class<E> edgeClass,
		final TraversalOptions options) {
		final Request request = executeTraversalRequest(options);
		return executor.execute(request, executeTraversalResponseDeserializer(vertexClass, edgeClass));
	}

	/**
	 * Reads a single document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document">API
	 *      Documentation</a>
	 * @param id
	 *            The id of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json)
	 * @return the document identified by the id
	 */
	public <T> CompletableFuture<T> getDocument(final String id, final Class<T> type) throws ArangoDBException {
		executor.validateDocumentId(id);
		final String[] split = id.split("/");
		return collection(split[0]).getDocument(split[1], type);
	}

	/**
	 * Reads a single document
	 * 
	 * @see <a href="https://docs.arangodb.com/current/HTTP/Document/WorkingWithDocuments.html#read-document">API
	 *      Documentation</a>
	 * @param id
	 *            The id of the document
	 * @param type
	 *            The type of the document (POJO class, VPackSlice or String for Json)
	 * @param options
	 *            Additional options, can be null
	 * @return the document identified by the id
	 */
	public <T> CompletableFuture<T> getDocument(final String id, final Class<T> type, final DocumentReadOptions options)
			throws ArangoDBException {
		executor.validateDocumentId(id);
		final String[] split = id.split("/");
		return collection(split[0]).getDocument(split[1], type, options);
	}

	/**
	 * Reload the routing table.
	 * 
	 * @see <a href=
	 *      "https://docs.arangodb.com/current/HTTP/AdministrationAndMonitoring/index.html#reloads-the-routing-information">API
	 *      Documentation</a>
	 * @return void
	 */
	public CompletableFuture<Void> reloadRouting() {
		return executor.execute(reloadRoutingRequest(), Void.class);
	}
}
