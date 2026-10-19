package com.yourbank.wealth;

import com.yourbank.wealth.client.Client;
import com.yourbank.wealth.client.ClientCategory;
import com.yourbank.wealth.identity.CurrentUser;
import com.yourbank.wealth.instrument.AssetClass;
import com.yourbank.wealth.instrument.Instrument;
import com.yourbank.wealth.portfolio.CashAccount;
import com.yourbank.wealth.portfolio.Portfolio;
import com.yourbank.wealth.portfolio.Position;
import com.yourbank.wealth.portfolio.ServiceType;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.test.util.ReflectionTestUtils;

/** Builds entities for unit tests (entities have no public setters by design). */
public final class TestData {

    private TestData() {
    }

    public static final CurrentUser RM1 = new CurrentUser("kc-rm1", "rm1", "Ravi Menon", null, List.of("RM"));
    public static final CurrentUser RM2 = new CurrentUser("kc-rm2", "rm2", "Rita Moore", null, List.of("RM"));
    public static final CurrentUser ADMIN = new CurrentUser("kc-admin", "admin1", "Anna Schmidt", null, List.of("ADMIN"));
    public static final CurrentUser CLIENT1 = new CurrentUser("kc-c1", "client1", "Clara Dupont", null, List.of("CLIENT"));

    public static Client client(long id, String rm, String portalUser) {
        Client c = instance(Client.class);
        ReflectionTestUtils.setField(c, "id", id);
        ReflectionTestUtils.setField(c, "clientNumber", "C-" + id);
        ReflectionTestUtils.setField(c, "firstName", "Test");
        ReflectionTestUtils.setField(c, "lastName", "Client" + id);
        ReflectionTestUtils.setField(c, "country", "FR");
        ReflectionTestUtils.setField(c, "category", ClientCategory.RETAIL);
        ReflectionTestUtils.setField(c, "rmUsername", rm);
        ReflectionTestUtils.setField(c, "portalUsername", portalUser);
        return c;
    }

    public static Portfolio portfolio(long id, Client client, ServiceType type) {
        Portfolio p = instance(Portfolio.class);
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "portfolioNumber", "P-" + id);
        ReflectionTestUtils.setField(p, "name", "Portfolio " + id);
        ReflectionTestUtils.setField(p, "client", client);
        ReflectionTestUtils.setField(p, "serviceType", type);
        ReflectionTestUtils.setField(p, "currency", "EUR");
        ReflectionTestUtils.setField(p, "openedOn", LocalDate.of(2024, 1, 1));
        return p;
    }

    public static Instrument instrument(long id, AssetClass type, String price, String contractSize) {
        Instrument i = instance(Instrument.class);
        ReflectionTestUtils.setField(i, "id", id);
        ReflectionTestUtils.setField(i, "code", "CODE" + id);
        ReflectionTestUtils.setField(i, "name", type + " " + id);
        ReflectionTestUtils.setField(i, "assetClass", type);
        ReflectionTestUtils.setField(i, "currency", "EUR");
        ReflectionTestUtils.setField(i, "riskClass", type.isDerivative() ? 7 : 4);
        ReflectionTestUtils.setField(i, "complex", type.isDerivative());
        ReflectionTestUtils.setField(i, "lastPrice", new BigDecimal(price));
        ReflectionTestUtils.setField(i, "priceDate", LocalDate.of(2026, 10, 16));
        if (contractSize != null) {
            ReflectionTestUtils.setField(i, "contractSize", new BigDecimal(contractSize));
        }
        return i;
    }

    public static Position position(Portfolio p, Instrument i, String qty, String avgCost) {
        return new Position(p, i, new BigDecimal(qty), new BigDecimal(avgCost));
    }

    public static CashAccount cash(Portfolio p, String balance) {
        CashAccount c = instance(CashAccount.class);
        ReflectionTestUtils.setField(c, "portfolio", p);
        ReflectionTestUtils.setField(c, "currency", "EUR");
        ReflectionTestUtils.setField(c, "balance", new BigDecimal(balance));
        return c;
    }

    private static <T> T instance(Class<T> type) {
        try {
            Constructor<T> ctor = type.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
