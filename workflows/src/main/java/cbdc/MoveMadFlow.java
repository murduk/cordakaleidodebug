package cbdc;

import co.paralleluniverse.fibers.Suspendable;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.contracts.utilities.AmountUtilities;
import com.r3.corda.lib.tokens.money.FiatCurrency;
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens;
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens;
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import com.r3.corda.lib.tokens.workflows.utilities.QueryUtilities;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
@InitiatingFlow
@StartableByRPC
public class MoveMadFlow extends FlowLogic<SignedTransaction> {
    @NotNull
    private final Party destination;
    private final long amount;

    public MoveMadFlow(@NotNull final Party destination, final long amount) {
        this.destination = destination;
        this.amount = amount;
    }

    @Override
    @Suspendable
    public SignedTransaction call() throws FlowException {
        // Prepare what we are talking about.
        final TokenType tokenType = FiatCurrency.getInstance("MAD");

        final Party bamMint = getServiceHub().getNetworkMapCache().getPeerByLegalName(MadTokenConstants.BAM_MINT);
        if (bamMint == null) throw new FlowException("No BAM Mint found");

        // Who is going to own the output, and how much?
        final Amount<TokenType> madAmount = AmountUtilities.amount(amount, tokenType);
        final PartyAndAmount<TokenType> destinationAmount = new PartyAndAmount<>(destination, madAmount);

        // Describe how to find those $ held by Me.
        final QueryCriteria issuedByBAMMint = QueryUtilities.tokenAmountWithIssuerCriteria(tokenType, bamMint);
        final QueryCriteria heldByMe = QueryUtilities.heldTokenAmountCriteria(tokenType, getOurIdentity());

        // Do the move
        return subFlow(new MoveFungibleTokens(
                Collections.singletonList(destinationAmount), // Output instances
                Collections.emptyList(), // Observers
                issuedByBAMMint.and(heldByMe), // Criteria to find the inputs
                getOurIdentity())); // change holder
    }
}
