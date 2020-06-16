package io.harness.engine.interrupts;

import com.google.inject.Inject;

import io.harness.interrupts.Interrupt;

public class InterruptManager {
  @Inject private InterruptHandlerFactory interruptHandlerFactory;

  public Interrupt register(InterruptPackage interruptPackage) {
    Interrupt interrupt = Interrupt.builder()
                              .planExecutionId(interruptPackage.getPlanExecutionId())
                              .type(interruptPackage.getInterruptType())
                              .createdBy(interruptPackage.getEmbeddedUser())
                              .nodeExecutionId(interruptPackage.getNodeExecutionId())
                              .parameters(interruptPackage.getParameters())
                              .build();
    InterruptHandler interruptHandler = interruptHandlerFactory.obtainHandler(interruptPackage.getInterruptType());
    return interruptHandler.registerInterrupt(interrupt);
  }
}
