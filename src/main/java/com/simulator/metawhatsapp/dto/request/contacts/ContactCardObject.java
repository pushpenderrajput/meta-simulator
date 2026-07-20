package com.simulator.metawhatsapp.dto.request.contacts;

import java.util.List;

public record ContactCardObject(
        NameObject name,
        List<PhoneObject> phones,
        List<EmailObject> emails,
        OrgObject org,
        List<UrlObject> urls,
        String birthday,
        List<AddressObject> addresses
) {
}