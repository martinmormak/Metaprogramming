package sk.tuke.meta.persistence.aspects;

import org.aspectj.lang.annotation.*;
import sk.tuke.meta.persistence.TransactionalPersistenceManager;

@Aspect
public class TransactionAspect {

    private static TransactionalPersistenceManager txManager;

    @Pointcut("execution(sk.tuke.meta.persistence.TransactionalPersistenceManager+.new(..))")
    public void transactionalManagerCreated() {}

    @After("execution(sk.tuke.meta.persistence.GeneratedPersistenceManager.new(..)) && this(manager)")
    public void capturePersistenceManager(TransactionalPersistenceManager manager) {
        System.out.println("✅ Captured PersistenceManager instance");
        txManager = manager;
    }

    @Pointcut("@annotation(sk.tuke.meta.persistence.annotations.AtomicPersistenceOperation)")
    public void transactionalMethod() {}

    @Before("transactionalMethod()")
    public void startTransaction() {
        System.out.println("👉 BEGIN TRANSACTION");
        if (txManager != null) txManager.beginTransaction();
    }

    @AfterReturning("transactionalMethod()")
    public void commitTransaction() {
        System.out.println("✅ COMMIT TRANSACTION");
        if (txManager != null) txManager.commitTransaction();
    }

    @AfterThrowing("transactionalMethod()")
    public void rollbackTransaction() {
        System.out.println("❌ ROLLBACK TRANSACTION");
        if (txManager != null) txManager.rollbackTransaction();
    }
}