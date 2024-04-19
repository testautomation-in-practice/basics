package example.spring.boot.helper

import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
open class SpringTest : MockedRepositoriesTest()