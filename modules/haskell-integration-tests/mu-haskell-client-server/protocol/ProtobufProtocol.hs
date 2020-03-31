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

module ProtobufProtocol where

import Data.Text as T
import GHC.Generics

import Mu.Quasi.GRpc
import Mu.Schema
import Mu.Schema.Optics

grpc "WeatherProtocol" id "weather.proto"

type GetForecastRequest = Term Maybe WeatherProtocol (WeatherProtocol :/: "GetForecastRequest")

type Weather = Term Maybe WeatherProtocol (WeatherProtocol :/: "Weather")

type GetForecastResponse = Term Maybe WeatherProtocol (WeatherProtocol :/: "GetForecastResponse")

type RainEvent = Term Maybe WeatherProtocol (WeatherProtocol :/: "RainEvent")

type RainEventType = Term Maybe WeatherProtocol (WeatherProtocol :/: "RainEventType")

started :: RainEventType
started = enum @"STARTED"

stopped :: RainEventType
stopped = enum @"STOPPED"

type RainSummaryResponse = Term Maybe WeatherProtocol (WeatherProtocol :/: "RainSummaryResponse")

type SubscribeToRainEventsRequest = Term Maybe WeatherProtocol (WeatherProtocol :/: "SubscribeToRainEventsRequest")

