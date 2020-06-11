package db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.persistence.NoResultException;

public class HibernateUtil {
    Session session = null;
    SessionFactory sessionFactory = new Configuration()
            .addAnnotatedClass(MainDB.class)
            .buildSessionFactory();

    public String getFolderByLoginAndPass(String login, String password) {
        try {
            session = sessionFactory.getCurrentSession();
            session.beginTransaction();
            MainDB query = session.createQuery("SELECT i FROM MainDB i WHERE i.login = :login AND i.password = :password", MainDB.class)
                    .setParameter("login", login)
                    .setParameter("password", password)
                    .getSingleResult();
            session.getTransaction().commit();
            return query.getFolderName();
        } catch (NoResultException e) {
            session.getTransaction().commit();
            return null;
        }
    }
}
