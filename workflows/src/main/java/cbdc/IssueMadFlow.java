package cbdc;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.money.MoneyUtilities;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
@InitiatingFlow
@StartableByRPC
public class IssueMadFlow extends FlowLogic<SignedTransaction> {
    @NotNull
    private final Party holder;
    private final long amount;

    public IssueMadFlow(@NotNull final Party holder, final long amount) {
        this.holder = holder;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        final Party issuer = getOurIdentity();
        if (!(issuer.getName().equals(MadTokenConstants.BAM_MINT) || issuer.getName().equals(MadTokenConstants.BAM_KALEIDO_MINT))) {
            throw new FlowException("We are not the BAM Mint");
        }

        TokenType tokenType;
        tokenType = FiatCurrency.getInstance("MAD");

        FungibleToken tokens = new FungibleTokenBuilder()
                .ofTokenType(tokenType)
                .withAmount(amount)
                .issuedBy(issuer)
                .heldBy(holder)
                .buildFungibleToken();

        return subFlow(new IssueTokens(Collections.singletonList(tokens)));
    }
}
