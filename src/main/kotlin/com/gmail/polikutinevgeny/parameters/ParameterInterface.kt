package com.gmail.polikutinevgeny.parameters

import com.gmail.polikutinevgeny.fields.FieldInterface

interface ParameterInterface {
    val value: FieldInterface
    val mask: FieldInterface
}