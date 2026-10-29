import {
  Alert,
  Anchor,
  Button,
  Checkbox,
  Group,
  Loader,
  Select,
  Stack,
  Text,
  TextInput,
  Title,
} from '@mantine/core';
import {
  EditConflictAlert,
  EditConflictError,
  usePageTitle,
  ValidationError,
} from '@orudesk/web-core';
import type { FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import type { NgoSchemas } from '../api';
import { DONOR_TYPES, useDonor, useEditDonor, type DonorType } from '../donors';

/**
 * Correct a donor (Day 50): the person's details and the NGO's, saved together with both versions read. The
 * PAN is never shown in full, so it can't be filled in: left empty it stays as it is, typed it's replaced, and
 * "Remove the PAN" takes it off.
 */
export function EditDonorPage() {
  const { donorId = '' } = useParams();
  const donor = useDonor(donorId);
  const edit = useEditDonor(donorId);
  const navigate = useNavigate();
  usePageTitle('Edit donor');
  const fieldErrors = edit.error instanceof ValidationError ? edit.error.fields : {};
  const conflict = edit.error instanceof EditConflictError;

  const reload = async () => {
    await donor.refetch();
    edit.reset();
  };

  if (donor.isPending) return <Loader size="sm" aria-label="Loading" />;
  if (donor.isError) return <Text role="alert">{donor.error.message}</Text>;
  if (donor.data === null) return <Title order={1}>Donor not found</Title>;
  const current = donor.data;

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const text = (name: string) => String(form.get(name) ?? '').trim();
    const optional = (name: string) => text(name) || null;
    const newPan = optional('pan')?.toUpperCase() ?? null;
    const changed: NgoSchemas['EditDonorRequest'] = {
      version: current.version,
      personVersion: current.personVersion,
      firstName: text('firstName'),
      lastName: text('lastName'),
      dateOfBirth: optional('dateOfBirth'),
      phone: optional('phone'),
      email: optional('email'),
      type: (optional('type') as DonorType | null) ?? current.type,
      // null keeps the PAN, "" removes it, a value replaces it (the API's rule)
      pan: form.get('removePan') === 'on' ? '' : newPan,
      wantsTaxReceipt: form.get('wantsTaxReceipt') === 'on',
      receivesUpdates: form.get('receivesUpdates') === 'on',
    };
    edit.mutate(changed, {
      onSuccess: () => void navigate(`/donors/${donorId}`, { replace: true }),
    });
  };

  return (
    <Stack maw={520}>
      <Anchor component={Link} to={`/donors/${donorId}`} size="sm">
        ← {current.firstName} {current.lastName}
      </Anchor>
      <Title order={1}>Edit donor</Title>
      {conflict && (
        <EditConflictAlert onReload={() => void reload()} reloading={donor.isFetching} />
      )}
      {edit.isError && !conflict && !(edit.error instanceof ValidationError) && (
        <Alert color="red" role="alert" title="Not saved">
          {edit.error.message}
        </Alert>
      )}
      {/* Two versions: either one moving on means the form shows old data, so both key the redraw */}
      <form key={`${current.version}-${current.personVersion}`} onSubmit={submit} noValidate>
        <Stack>
          <Group grow align="flex-start">
            <TextInput
              name="firstName"
              label="First name"
              withAsterisk
              defaultValue={current.firstName}
              error={fieldErrors.firstName}
            />
            <TextInput
              name="lastName"
              label="Last name"
              withAsterisk
              defaultValue={current.lastName}
              error={fieldErrors.lastName}
            />
          </Group>
          <Select
            name="type"
            label="Type"
            data={DONOR_TYPES}
            defaultValue={current.type}
            allowDeselect={false}
            maw={220}
          />
          <TextInput
            name="dateOfBirth"
            label="Date of birth"
            type="date"
            maw={200}
            defaultValue={current.dateOfBirth ?? ''}
            error={fieldErrors.dateOfBirth}
          />
          <TextInput
            name="phone"
            label="Phone"
            type="tel"
            defaultValue={current.phone ?? ''}
            error={fieldErrors.phone}
          />
          <TextInput
            name="email"
            label="Email"
            type="email"
            defaultValue={current.email ?? ''}
            error={fieldErrors.email}
          />
          <TextInput
            name="pan"
            label="New PAN"
            description={
              current.panMasked
                ? `Now ${current.panMasked}. Leave empty to keep it.`
                : 'None given yet. Needed for an 80G tax receipt.'
            }
            maw={260}
            error={fieldErrors.pan}
          />
          {current.panMasked && <Checkbox name="removePan" label="Remove the PAN" />}
          <Checkbox
            name="wantsTaxReceipt"
            label="Wants 80G tax receipts"
            defaultChecked={current.wantsTaxReceipt}
          />
          <Checkbox
            name="receivesUpdates"
            label="Receives updates by email"
            defaultChecked={current.receivesUpdates}
          />
          <Group>
            <Button type="submit" loading={edit.isPending} disabled={conflict}>
              Save
            </Button>
            <Button component={Link} to={`/donors/${donorId}`} variant="default">
              Cancel
            </Button>
          </Group>
        </Stack>
      </form>
    </Stack>
  );
}
