import { coreModule, startOrudeskApp } from '@orudesk/web-core';
import { ngoModule } from './ngoModule';

// The NGO edition: everything the core has, plus donors
startOrudeskApp([coreModule, ngoModule]);
