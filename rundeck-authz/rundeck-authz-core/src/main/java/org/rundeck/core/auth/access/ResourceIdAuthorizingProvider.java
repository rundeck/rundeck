package org.rundeck.core.auth.access;

/**
 * Accessor for an authorized resource of a certain type and ID
 *
 * @param <T> resource type
 * @param <A> authorized resource type
 * @param <I> ID type
 */
public interface ResourceIdAuthorizingProvider<T, A extends AuthorizingIdResource<T, I>, I> extends
                                                                                            AuthorizingAccessProvider<A>
{

}
