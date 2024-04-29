package com.datasqrl.graphql.server;

import com.datasqrl.graphql.server.RootGraphqlModel.Argument;
import com.datasqrl.graphql.server.RootGraphqlModel.MutationCoords;
import com.datasqrl.graphql.server.RootGraphqlModel.ResolvedQuery;
import com.datasqrl.graphql.server.RootGraphqlModel.SubscriptionCoords;
import graphql.schema.DataFetcher;
import java.util.Map;
import java.util.Set;

//  @Value
public interface Context {

  JdbcClient getClient();

  DataFetcher<Object> createPropertyFetcher(String name);

  DataFetcher<?> createArgumentLookupFetcher(GraphQLEngineBuilder server, Map<Set<Argument>, ResolvedQuery> lookupMap);

  DataFetcher<?> createSinkFetcher(MutationCoords coords);

  DataFetcher<?> createSubscriptionFetcher(SubscriptionCoords coords, Map<String, String> filters);
}
