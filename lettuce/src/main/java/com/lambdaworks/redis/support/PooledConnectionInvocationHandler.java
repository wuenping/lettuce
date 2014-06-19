package com.lambdaworks.redis.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.AbstractInvocationHandler;
import com.lambdaworks.redis.RedisConnectionPool;
import com.lambdaworks.redis.RedisException;

/**
 * Invocation handler which takes care of connection.close(). Connections are returned to the pool on a close()-call.
 * 
 * @author <a href="mailto:mpaluch@paluch.biz">Mark Paluch</a>
 * @param <T> Connection type.
 * @since 23.05.14 22:14
 */
public class PooledConnectionInvocationHandler<T> extends AbstractInvocationHandler {
    public final static Set<String> DISABLED_METHODS = ImmutableSet.of("auth", "select", "quit");

    private T connection;
    private final RedisConnectionPool<T> pool;

    public PooledConnectionInvocationHandler(T connection, RedisConnectionPool<T> pool) {
        this.connection = connection;
        this.pool = pool;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object handleInvocation(Object proxy, Method method, Object[] args) throws Throwable {

        if (DISABLED_METHODS.contains(method.getName())) {
            throw new UnsupportedOperationException("Calls to " + method.getName() + " are not supported on pooled connections");
        }

        if (connection == null) {
            throw new RedisException("Connection is deallocated and cannot be used anymore.");
        }

        if (method.getName().equals("close")) {
            pool.freeConnection((T) proxy);
            connection = null;
            return null;
        }

        Method targetMethod = connection.getClass().getMethod(method.getName(), method.getParameterTypes());
        try {
            return targetMethod.invoke(connection, args);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    public T getConnection() {
        return connection;
    }
}