package cbdc.tests;

import cbdc.IssueMadFlow;
import cbdc.MadTokenConstants;
import cbdc.MoveMadFlow;
import cbdc.QueryMadFlow;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import kotlin.Pair;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.TransactionResolutionException;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.common.internal.ParametersUtilitiesKt;
import net.corda.testing.node.*;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class FlowTests {
    private MockNetwork network;
    private StartedMockNode a;
    private StartedMockNode b;
    private StartedMockNode c;

    @Before
    public void setup() {
        network = new MockNetwork(
                new MockNetworkParameters()
                        .withCordappsForAllNodes(ImmutableList.of(
                                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"),
                                TestCordapp.findCordapp("cbdc")
                        ))
                        .withNetworkParameters(ParametersUtilitiesKt.testNetworkParameters(
                                Collections.emptyList(), 4
                        ))
                        .withNotarySpecs(ImmutableList.of(new MockNetworkNotarySpec(CordaX500Name.parse("O=Notary,L=London,C=GB"))))
        );
        a = network.createPartyNode(MadTokenConstants.BAM_MINT);
        b = network.createPartyNode(null);
        c = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @SuppressWarnings("UnusedReturnValue")
    @NotNull
    private SignedTransaction aIssues100ToB() throws Exception {
        Party partyB = b.getInfo().getLegalIdentities().get(0);
        IssueMadFlow issueMadFlow = new IssueMadFlow(partyB, 100);
        final CordaFuture<SignedTransaction> future = a.startFlow(issueMadFlow);
        network.runNetwork();
        return future.get();
    }

    @SuppressWarnings("UnusedReturnValue")
    @NotNull
    private SignedTransaction bMoves60toC() throws Exception {
        Party partyC = c.getInfo().getLegalIdentities().get(0);
        MoveMadFlow moveMadFlow = new MoveMadFlow(partyC, 60);
        final CordaFuture<SignedTransaction> future = b.startFlow(moveMadFlow);
        network.runNetwork();
        return future.get();
    }

    @NotNull
    private BigDecimal checkBalanceB() throws Exception {
        QueryMadFlow queryMadFlow = new QueryMadFlow();
        final CordaFuture<BigDecimal> future = b.startFlow(queryMadFlow);
        network.runNetwork();
        return future.get();
    }

    /**
     * Test will issue MAD currency and check that it is committed to ledger of the holder
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void issueMadCurrency() throws Exception {
        aIssues100ToB();
        VaultService vs = b.getServices().getVaultService();
        // Use QueryUtilities to easily run various queries related to tokens
        TokenType tokenType;
        tokenType = FiatCurrency.getInstance("MAD");
        Amount<TokenType> fiatBalance = QueryUtilities.tokenBalance(vs, tokenType);
        assert (10000L == fiatBalance.getQuantity()); // Quantities in tokenTypes are stored as long representations with the fractional digits
    }

    /**
     * Test will move MAD currency and check that it is committed to ledger of the holder and destination
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Test
    public void moveMadCurrency() throws Exception {
        aIssues100ToB();
        bMoves60toC();
        VaultService vsB = b.getServices().getVaultService();
        VaultService vsC = c.getServices().getVaultService();
        // Use QueryUtilities to easily run various queries related to tokens
        TokenType tokenType;
        tokenType = FiatCurrency.getInstance("MAD");
        //assert
        Amount<TokenType> bBalance = QueryUtilities.tokenBalance(vsB, tokenType);
        Amount<TokenType> cBalance = QueryUtilities.tokenBalance(vsC, tokenType);
        assert (4000L == bBalance.getQuantity());
        assert (6000L == cBalance.getQuantity());
    }

    @Test
    public void checkBalanceOfB() throws Exception {
        aIssues100ToB();
        BigDecimal result = checkBalanceB();
        VaultService vs = b.getServices().getVaultService();
        // Use QueryUtilities to easily run various queries related to tokens
        TokenType tokenType;
        tokenType = FiatCurrency.getInstance("MAD");
        Amount<TokenType> fiatBalance = QueryUtilities.tokenBalance(vs, tokenType);
        assertEquals(fiatBalance.toDecimal(), result);
    }

}