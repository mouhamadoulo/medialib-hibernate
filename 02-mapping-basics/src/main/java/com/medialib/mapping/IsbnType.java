package com.medialib.mapping;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.UserType;

/**
 * {@link UserType} pour {@link Isbn}.
 *
 * <p>Quand un {@code AttributeConverter} suffit-il, et quand faut-il un {@code UserType} ?
 * Le converter se contente de traduire valeur ↔ colonne. Le {@code UserType} controle en plus
 * le type SQL, la comparaison, la copie et la serialisation dans le cache de second niveau —
 * donc tout ce dont Hibernate a besoin pour decider qu'une entite est « sale ».
 */
public class IsbnType implements UserType<Isbn> {

    @Override
    public int getSqlType() {
        return Types.VARCHAR;
    }

    @Override
    public Class<Isbn> returnedClass() {
        return Isbn.class;
    }

    @Override
    public boolean equals(Isbn x, Isbn y) {
        return java.util.Objects.equals(x, y);
    }

    @Override
    public int hashCode(Isbn x) {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Isbn nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws SQLException {
        String value = rs.getString(position);
        return value == null ? null : new Isbn(value);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Isbn value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            st.setString(index, value.value()); // deja normalise par le record
        }
    }

    @Override
    public Isbn deepCopy(Isbn value) {
        return value; // record immuable
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Isbn value) {
        return value;
    }

    @Override
    public Isbn assemble(Serializable cached, Object owner) {
        return (Isbn) cached;
    }
}
