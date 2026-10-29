import { Alert, Button, Checkbox, Group, Select, Stack, TextInput, Title } from '@mantine/core';
import { usePageTitle, ValidationError } from '@orudesk/web-core';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import type { NgoSchemas } from '../api';
import { DONOR_TYPES, useRegisterDonor, type DonorType } from '../donors';

/** Add a donor, and with them a person in the core's registry. The server checks everything. */
export function NewDonorPage() {
  usePageTitle('Add donor');
  const register = useRegisterDonor();
  const navigate = useNavigate();
  const fieldErrors = register.error instanceof ValidationError ? register.error.fields : {};

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? '').trim();
    const optional = (name: string) => text(name) || null;
    const donor: NgoSchemas['RegisterDonorRequest'] = {
      firstName: text('firstName'),
      lastName: text('lastName'),
      dateOfBirth: optional('dateOfBirth'),
      phone: optional('phone'),
      email: optional('email'),
      type: (optional('type') as DonorType | null) ?? 'INDIVIDUAL',
      pan: optional('pan')?.toUpperCase() ?? null,
      wantsTaxReceipt: form.get('wantsTaxReceipt') === 'on',
      receivesUpdates: form.get('receivesUpdates') === 'on',
    };
    register.mutate(donor, {
      onSuccess: (id) => void navigate(`/donors/${id}`, { replace: true }),
    });
  };

  return (
    <Stack maw={520}>
      <Title order={1}>Add donor</Title>
      {register.isError && !(register.error instanceof ValidationError) && (
        <Alert color="red" role="alert" title="Not saved">
          {register.error.message}
        </Alert>
      )}
      <form onSubmit={submit} noValidate>
        <Stack>
          <Group grow align="flex-start">
            <TextInput
              name="firstName"
              label="First name"
              withAsterisk
              error={fieldErrors.firstName}
            />
            <TextInput
              name="lastName"
              label="Last name"
              withAsterisk
              error={fieldErrors.lastName}
            />
          </Group>
          <Select
            name="type"
            label="Type"
            data={DONOR_TYPES}
            defaultValue="INDIVIDUAL"
            allowDeselect={false}
            maw={220}
            description="For a company or foundation, the name is the contact person's"
          />
          <TextInput
            name="dateOfBirth"
            label="Date of birth"
            type="date"
            maw={200}
            error={fieldErrors.dateOfBirth}
          />
          <TextInput name="phone" label="Phone" type="tel" error={fieldErrors.phone} />
          <TextInput name="email" label="Email" type="email" error={fieldErrors.email} />
          <TextInput
            name="pan"
            label="PAN"
            description="Needed for an 80G tax receipt"
            maw={220}
            error={fieldErrors.pan}
          />
          <Checkbox name="wantsTaxReceipt" label="Wants 80G tax receipts" />
          <Checkbox name="receivesUpdates" label="Receives updates by email" />
          <Group>
            <Button type="submit" loading={register.isPending}>
              Save
            </Button>
            <Button component={Link} to="/donors" variant="default">
              Cancel
            </Button>
          </Group>
        </Stack>
      </form>
    </Stack>
  );
}
