{-# language DataKinds             #-}
{-# language DeriveAnyClass        #-}
{-# language DeriveGeneric         #-}
{-# language DuplicateRecordFields #-}
{-# language FlexibleContexts      #-}
{-# language FlexibleInstances     #-}
{-# language MultiParamTypeClasses #-}
{-# language PolyKinds             #-}
{-# language TemplateHaskell       #-}
{-# language TypeApplications      #-}
{-# language TypeFamilies          #-}
{-# language TypeOperators         #-}

module AvroProtocol where

import Data.Functor.Identity
import GHC.Generics

import Mu.Quasi.Avro
import Mu.Schema
import Mu.Schema.Optics

avdl "WeatherProtocol" "WeatherService" "." "weather.avdl"

type GetForecastRequest = Term Identity WeatherProtocol (WeatherProtocol :/: "GetForecastRequest")

type Weather = Term Identity WeatherProtocol (WeatherProtocol :/: "Weather")

type GetForecastResponse = Term Identity WeatherProtocol (WeatherProtocol :/: "GetForecastResponse")
