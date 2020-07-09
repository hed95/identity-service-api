package io.digital.patterns.identity.api.audit

import spock.lang.Specification

class AuditListenerSpec extends Specification {

   def underTest = new AuditEventListener()


   def 'can handle event'() {
       given: 'an event'
       def event = new AuditEvent(
               'path',
               'userId',
               new Date(),
               'ipAddress',
               'GET',
               'auth'
       )

       expect: 'listener can log'
       underTest.handle(event)
   }
}
