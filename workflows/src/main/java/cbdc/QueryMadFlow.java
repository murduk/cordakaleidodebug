package cbdc;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.VaultService;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Collections;

@InitiatingFlow
@StartableByRPC
public class QueryMadFlow extends FlowLogic<BigDecimal> {
    public QueryMadFlow() {
    }
    @Override
    @Suspendable
    public BigDecimal call() throws FlowException {
        final Party us = getOurIdentity();
        VaultService vaultService = getServiceHub().getVaultService();
        TokenType tokenType;
        tokenType = FiatCurrency.getInstance("MAD");
        Amount<TokenType> fiatBalance = QueryUtilities.tokenBalance(vaultService, tokenType);
        return fiatBalance.toDecimal();
    }
}
