package at.ac.fhcampus.master.dtos

import java.util


class RegisterUser(var accountNonExpired: Boolean,
                   var accountNonLocked: Boolean,
                   var credentialsNonExpired: Boolean,
                   var enabled: Boolean,
                   var username: String,
                   var password: String,
                   var roles: util.ArrayList[Role])
