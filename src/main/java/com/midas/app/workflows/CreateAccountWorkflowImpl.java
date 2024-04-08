package com.midas.app.workflows;

import com.midas.app.activities.AccountActivity;
import com.midas.app.models.Account;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@WorkflowImpl(taskQueues = "create-account-workflow")
public class CreateAccountWorkflowImpl implements CreateAccountWorkflow {

  private final RetryOptions retryoptions =
      RetryOptions.newBuilder()
          .setInitialInterval(Duration.ofSeconds(1))
          .setMaximumInterval(Duration.ofSeconds(100))
          .setBackoffCoefficient(2)
          .setMaximumAttempts(50000)
          .build();
  private final ActivityOptions options =
      ActivityOptions.newBuilder()
          .setStartToCloseTimeout(Duration.ofSeconds(30))
          .setRetryOptions(retryoptions)
          .build();

  private final AccountActivity activity = Workflow.newActivityStub(AccountActivity.class, options);

  @Override
  public Account createAccount(Account details) {
    Account account = activity.saveAccount(details);
    log.info("Account Created Successfully");

    Account paymentAccount = activity.createPaymentAccount(account);
    account.setPaymentProviderType(paymentAccount.getPaymentProviderType());
    account.setProviderId(paymentAccount.getProviderId());

    log.info("Updating Payment Details In Account");
    return activity.saveAccount(account);
  }
}
